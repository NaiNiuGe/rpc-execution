package com.leo.rpc.core.transport.server;

import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.core.provider.DefaultServiceProvider;
import com.leo.rpc.core.provider.ServiceProvider;
import com.leo.rpc.core.registry.ServiceRegistry;
import com.leo.rpc.core.registry.nacos.NacosServiceRegistry;
import com.leo.rpc.core.serialize.KryoSerializer;
import com.leo.rpc.core.serialize.Serializer;
import com.leo.rpc.core.transport.codec.RpcDecoder;
import com.leo.rpc.core.transport.codec.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty服务端
 * <p>
 * 启动Netty Server监听端口，接收RPC请求。
 * 服务注册到Nacos，本地维护ServiceProvider映射。
 */
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    private final String host;
    private final int port;
    private final ServiceRegistry serviceRegistry;
    private final ServiceProvider serviceProvider;
    private final Serializer serializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(String host, int port, String nacosAddress) {
        this.host = host;
        this.port = port;
        this.serviceRegistry = new NacosServiceRegistry(nacosAddress);
        this.serviceProvider = new DefaultServiceProvider();
        this.serializer = new KryoSerializer();
    }

    /**
     * 发布服务
     * <p>
     * 同时注册到本地ServiceProvider和Nacos注册中心
     *
     * @param service       服务实现对象
     * @param serviceClass  服务接口Class
     * @param version       版本号
     * @param group         分组
     */
    public void publishService(Object service, Class<?> serviceClass, String version, String group) {
        String serviceName = serviceClass.getName() + "#" + version + "#" + group;
        // 注册到本地
        serviceProvider.addService(serviceName, service);
        // 注册到Nacos
        serviceRegistry.registerService(serviceName, new InetSocketAddress(host, port));
    }

    /**
     * 发布服务（使用默认版本和分组）
     */
    public void publishService(Object service, Class<?> serviceClass) {
        publishService(service, serviceClass, "", "");
    }

    /**
     * 启动Netty服务器
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 服务端接受连接的队列长度
                    .option(ChannelOption.SO_BACKLOG, 256)
                    // TCP保活
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 禁用Nagle算法，减少延迟
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 空闲检测（30秒无读操作则关闭连接）
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 解码器
                            pipeline.addLast(new RpcDecoder(serializer));
                            // 编码器
                            pipeline.addLast(new RpcEncoder(serializer));
                            // 业务处理器
                            pipeline.addLast(new NettyServerHandler(serviceProvider));
                        }
                    });

            // 绑定端口，同步等待
            ChannelFuture future = bootstrap.bind(host, port).sync();
            log.info("======== RPC服务端启动成功 ========");
            log.info("监听地址: {}:{}", host, port);
            log.info("==================================");

            // 注册JVM关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            // 等待服务端Channel关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException("服务端启动失败", e);
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        log.info("关闭Netty服务端...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
