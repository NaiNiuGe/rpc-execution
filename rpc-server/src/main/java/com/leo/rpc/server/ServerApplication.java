package com.leo.rpc.server;

import com.leo.rpc.api.HelloService;
import com.leo.rpc.core.transport.server.NettyServer;
import com.leo.rpc.server.service.HelloServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC服务端启动类（示例）
 * <p>
 * 使用步骤：
 * 1. 确保Nacos已启动 (默认地址 127.0.0.1:8848)
 * 2. 运行此类启动RPC服务端
 */
public class ServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        // 配置参数
        String host = "127.0.0.1";
        int port = 9090;
        String nacosAddress = "127.0.0.1:8848";

        // 支持通过命令行参数自定义
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            nacosAddress = args[2];
        }

        log.info("========================================");
        log.info("  RPC Execution - 服务端启动中...");
        log.info("  Host: {}", host);
        log.info("  Port: {}", port);
        log.info("  Nacos: {}", nacosAddress);
        log.info("========================================");

        // 创建Netty服务器
        NettyServer server = new NettyServer(host, port, nacosAddress);

        // 发布服务
        server.publishService(new HelloServiceImpl(), HelloService.class);

        // 启动服务器（阻塞）
        server.start();
    }
}
