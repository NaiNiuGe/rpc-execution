package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.registry.ServiceDiscovery;
import com.leo.rpc.core.serialize.KryoSerializer;
import com.leo.rpc.core.serialize.Serializer;
import com.leo.rpc.core.transport.codec.RpcDecoder;
import com.leo.rpc.core.transport.codec.RpcEncoder;
import com.leo.rpc.core.transport.codec.RpcProtocolConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty client.
 */
public class NettyClient {

    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final long requestTimeoutMillis;

    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    private final Map<String, String> channelIdToAddressKey = new ConcurrentHashMap<>();
    private final Map<String, String> requestIdToChannelId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> channelIdToRequestIds = new ConcurrentHashMap<>();

    public NettyClient(ServiceDiscovery serviceDiscovery) {
        this(serviceDiscovery, RpcProtocolConstants.REQUEST_TIMEOUT_MILLIS);
    }

    public NettyClient(ServiceDiscovery serviceDiscovery, long requestTimeoutMillis) {
        this.serviceDiscovery = serviceDiscovery;
        this.unprocessedRequests = new UnprocessedRequests();
        this.eventLoopGroup = new NioEventLoopGroup();
        this.requestTimeoutMillis = requestTimeoutMillis;

        Serializer serializer = new KryoSerializer();

        this.bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new io.netty.channel.ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS))
                                .addLast(new RpcDecoder(serializer))
                                .addLast(new RpcEncoder(serializer))
                                .addLast(new NettyClientHandler(NettyClient.this));
                    }
                });
    }

    public CompletableFuture<RpcResponse<?>> sendRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<?>> resultFuture = new CompletableFuture<>();
        InetSocketAddress address = serviceDiscovery.discoverService(rpcRequest.getServiceName());
        Channel channel = getChannel(address);

        if (channel == null || !channel.isActive()) {
            throw new RpcException("Connection is unavailable: " + address);
        }

        trackRequest(channel, rpcRequest.getRequestId(), resultFuture);
        channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("Request sent successfully: requestId={}", rpcRequest.getRequestId());
                        scheduleRequestTimeout(channel, rpcRequest.getRequestId(),
                                requestTimeoutMillis + RpcProtocolConstants.REQUEST_TIMEOUT_CLEANUP_DELAY_MILLIS);
            } else {
                releaseRequest(rpcRequest.getRequestId(), future.cause());
                future.channel().close();
                log.error("Request send failed: requestId={}", rpcRequest.getRequestId(), future.cause());
            }
        });

        return resultFuture;
    }

    void completeResponse(Channel channel, RpcResponse<?> response) {
        if (response == null || response.getRequestId() == null) {
            log.warn("Received empty response");
            return;
        }

        CompletableFuture<RpcResponse<?>> future = detachRequest(response.getRequestId());
        if (future == null) {
            log.debug("Ignore stale or duplicate response: requestId={}", response.getRequestId());
            return;
        }
        future.complete(response);
    }

    void sendHeartbeat(Channel channel) {
        if (channel == null || !channel.isActive()) {
            return;
        }

        String requestId = UUID.randomUUID().toString();
        RpcRequest heartbeatRequest = RpcRequest.builder()
                .requestId(requestId)
                .interfaceName(RpcProtocolConstants.HEARTBEAT_INTERFACE_NAME)
                .methodName(RpcProtocolConstants.HEARTBEAT_METHOD_NAME)
                .paramTypes(new Class<?>[0])
                .params(new Object[0])
                .version(RpcProtocolConstants.HEARTBEAT_VERSION)
                .group(RpcProtocolConstants.HEARTBEAT_GROUP)
                .build();

        CompletableFuture<RpcResponse<?>> future = new CompletableFuture<>();
        trackRequest(channel, requestId, future);

        channel.writeAndFlush(heartbeatRequest).addListener((ChannelFutureListener) writeFuture -> {
            if (writeFuture.isSuccess()) {
                log.debug("Heartbeat sent: channel={}", channel.remoteAddress());
                scheduleRequestTimeout(channel, requestId, RpcProtocolConstants.HEARTBEAT_TIMEOUT_MILLIS);
            } else {
                releaseRequest(requestId, writeFuture.cause());
                log.warn("Heartbeat send failed: channel={}", channel.remoteAddress(), writeFuture.cause());
                writeFuture.channel().close();
            }
        });
    }

    void handleChannelClosed(Channel channel, Throwable cause) {
        if (channel == null) {
            return;
        }

        String channelId = channelId(channel);
        String addressKey = channelIdToAddressKey.remove(channelId);
        if (addressKey != null) {
            channelMap.remove(addressKey, channel);
        }

        Set<String> requestIds = channelIdToRequestIds.remove(channelId);
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        Throwable failure = cause != null ? cause : new RpcException("Connection closed: " + channel.remoteAddress());
        for (String requestId : requestIds) {
            releaseRequest(requestId, failure);
        }
    }

    public void shutdown() {
        log.info("Shutting down Netty client...");
        failAllPendingRequests(new RpcException("Client shutdown"));
        channelMap.values().forEach(Channel::close);
        channelMap.clear();
        channelIdToAddressKey.clear();
        eventLoopGroup.shutdownGracefully();
    }

    private Channel getChannel(InetSocketAddress address) {
        String addressKey = address.toString();
        Channel channel = channelMap.get(addressKey);
        if (isActive(channel)) {
            return channel;
        }

        synchronized (channelMap) {
            channel = channelMap.get(addressKey);
            if (isActive(channel)) {
                return channel;
            }

            if (channel != null) {
                channelMap.remove(addressKey, channel);
            }

            channel = connect(address, addressKey);
            channelMap.put(addressKey, channel);
            return channel;
        }
    }

    private Channel connect(InetSocketAddress address, String addressKey) {
        try {
            ChannelFuture future = bootstrap.connect(address).sync();
            Channel channel = future.channel();
            channelIdToAddressKey.put(channelId(channel), addressKey);
            channel.closeFuture().addListener(closeFuture -> handleChannelClosed(channel, null));
            log.info("Connected to service node: {}", address);
            return channel;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException("Connect failed: " + address, e);
        }
    }

    private boolean isActive(Channel channel) {
        return channel != null && channel.isActive();
    }

    private String channelId(Channel channel) {
        return channel.id().asLongText();
    }

    private void trackRequest(Channel channel, String requestId, CompletableFuture<RpcResponse<?>> future) {
        unprocessedRequests.put(requestId, future);
        String channelId = channelId(channel);
        requestIdToChannelId.put(requestId, channelId);
        channelIdToRequestIds.computeIfAbsent(channelId, key -> ConcurrentHashMap.newKeySet()).add(requestId);
        future.whenComplete((response, throwable) -> detachRequest(requestId));
    }

    private CompletableFuture<RpcResponse<?>> detachRequest(String requestId) {
        String channelId = requestIdToChannelId.remove(requestId);
        if (channelId != null) {
            Set<String> requestIds = channelIdToRequestIds.get(channelId);
            if (requestIds != null) {
                requestIds.remove(requestId);
                if (requestIds.isEmpty()) {
                    channelIdToRequestIds.remove(channelId, requestIds);
                }
            }
        }
        return unprocessedRequests.remove(requestId);
    }

    private void releaseRequest(String requestId, Throwable cause) {
        CompletableFuture<RpcResponse<?>> future = detachRequest(requestId);
        if (future != null && cause != null && !future.isDone()) {
            future.completeExceptionally(cause);
        }
    }

    private void scheduleRequestTimeout(Channel channel, String requestId, long timeoutMillis) {
        channel.eventLoop().schedule(() -> {
            CompletableFuture<RpcResponse<?>> future = detachRequest(requestId);
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new RpcException("RPC request timeout: " + requestId));
                log.warn("Request timeout cleaned up: requestId={}", requestId);
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private void failAllPendingRequests(Throwable cause) {
        for (String requestId : new ArrayList<>(requestIdToChannelId.keySet())) {
            releaseRequest(requestId, cause);
        }
        requestIdToChannelId.clear();
        channelIdToRequestIds.clear();
    }
}
