package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.registry.ServiceDiscovery;
import com.leo.rpc.core.serialize.KryoSerializer;
import com.leo.rpc.core.serialize.Serializer;
import com.leo.rpc.core.transport.codec.RpcDecoder;
import com.leo.rpc.core.transport.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty客户端
 * <p>
 * 负责与服务端建立TCP连接，发送RPC请求并异步接收响应。
 * 支持连接复用（Channel缓存）。
 */
public class NettyClient {

    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    /**
     * 连接缓存：地址 -> Channel
     */
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public NettyClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
        this.unprocessedRequests = new UnprocessedRequests();
        this.eventLoopGroup = new NioEventLoopGroup();

        Serializer serializer = new KryoSerializer();

        this.bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 空闲检测
                        pipeline.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        // 解码器
                        pipeline.addLast(new RpcDecoder(serializer));
                        // 编码器
                        pipeline.addLast(new RpcEncoder(serializer));
                        // 业务处理器
                        pipeline.addLast(new NettyClientHandler(unprocessedRequests));
                    }
                });
    }

    /**
     * 发送RPC请求（异步）
     *
     * @param rpcRequest RPC请求
     * @return CompletableFuture<RpcResponse < ?>>
     */
    public CompletableFuture<RpcResponse<?>> sendRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<?>> resultFuture = new CompletableFuture<>();
        // 服务发现
        InetSocketAddress address = serviceDiscovery.discoverService(rpcRequest.getServiceName());
        // 获取或创建Channel
        Channel channel = getChannel(address);

        if (channel != null && channel.isActive()) {
            // 注册Future
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            // 发送请求
            channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("请求发送成功: requestId={}", rpcRequest.getRequestId());
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    unprocessedRequests.remove(rpcRequest.getRequestId());
                    log.error("请求发送失败: requestId={}", rpcRequest.getRequestId(), future.cause());
                }
            });
        } else {
            throw new RpcException("连接不可用: " + address);
        }

        return resultFuture;
    }

    /**
     * 获取Channel，支持连接复用
     */
    private Channel getChannel(InetSocketAddress address) {
        String key = address.toString();
        Channel channel = channelMap.get(key);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        // 移除失效连接
        channelMap.remove(key);
        // 建立新连接
        channel = connect(address);
        channelMap.put(key, channel);
        return channel;
    }

    /**
     * 建立TCP连接
     */
    private Channel connect(InetSocketAddress address) {
        try {
            ChannelFuture future = bootstrap.connect(address).sync();
            log.info("连接服务端成功: {}", address);
            return future.channel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException("连接服务端失败: " + address, e);
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        log.info("关闭Netty客户端...");
        channelMap.values().forEach(Channel::close);
        channelMap.clear();
        eventLoopGroup.shutdownGracefully();
    }
}
