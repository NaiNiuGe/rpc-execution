package com.leo.rpc.server;

import com.leo.rpc.api.HelloService;
import com.leo.rpc.core.config.RpcConfig;
import com.leo.rpc.core.transport.server.NettyServer;
import com.leo.rpc.server.service.HelloServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC server example entry point.
 */
public class ServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        RpcConfig rpcConfig = RpcConfig.loadFromClasspath();
        applyCommandLineOverrides(rpcConfig, args);

        log.info("========================================");
        log.info("RPC server starting");
        log.info("Host: {}", rpcConfig.getServerHost());
        log.info("Port: {}", rpcConfig.getServerPort());
        log.info("Nacos: {}", rpcConfig.getNacosAddress());
        log.info("========================================");

        NettyServer server = new NettyServer(
                rpcConfig.getServerHost(),
                rpcConfig.getServerPort(),
                rpcConfig.getNacosAddress());

        server.publishService(new HelloServiceImpl(), HelloService.class);
        server.start();
    }

    private static void applyCommandLineOverrides(RpcConfig rpcConfig, String[] args) {
        if (args.length >= 1 && !args[0].trim().isEmpty()) {
            rpcConfig.setServerHost(args[0].trim());
        }
        if (args.length >= 2 && !args[1].trim().isEmpty()) {
            rpcConfig.setServerPort(Integer.parseInt(args[1].trim()));
        }
        if (args.length >= 3 && !args[2].trim().isEmpty()) {
            rpcConfig.setNacosAddress(args[2].trim());
        }
    }
}
