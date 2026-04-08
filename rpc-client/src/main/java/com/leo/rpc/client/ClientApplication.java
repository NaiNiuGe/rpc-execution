package com.leo.rpc.client;

import com.leo.rpc.api.HelloService;
import com.leo.rpc.api.model.User;
import com.leo.rpc.core.config.RpcConfig;
import com.leo.rpc.core.loadbalance.LoadBalancer;
import com.leo.rpc.core.loadbalance.RandomLoadBalancer;
import com.leo.rpc.core.loadbalance.RoundRobinLoadBalancer;
import com.leo.rpc.core.proxy.RpcClientProxy;
import com.leo.rpc.core.registry.nacos.NacosServiceDiscovery;
import com.leo.rpc.core.transport.client.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * RPC client example entry point.
 */
public class ClientApplication {

    private static final Logger log = LoggerFactory.getLogger(ClientApplication.class);

    public static void main(String[] args) {
        RpcConfig rpcConfig = RpcConfig.loadFromClasspath();
        applyCommandLineOverrides(rpcConfig, args);

        log.info("========================================");
        log.info("RPC client starting");
        log.info("Nacos: {}", rpcConfig.getNacosAddress());
        log.info("Timeout: {} ms", rpcConfig.getRequestTimeout());
        log.info("LoadBalance: {}", rpcConfig.getLoadBalance());
        log.info("========================================");

        LoadBalancer loadBalancer = createLoadBalancer(rpcConfig.getLoadBalance());
        NacosServiceDiscovery serviceDiscovery = new NacosServiceDiscovery(
                rpcConfig.getNacosAddress(),
                loadBalancer);

        NettyClient nettyClient = new NettyClient(serviceDiscovery, rpcConfig.getRequestTimeout());
        RpcClientProxy proxy = new RpcClientProxy(nettyClient, "", "", rpcConfig.getRequestTimeout());
        HelloService helloService = proxy.getProxy(HelloService.class);

        try {
            log.info("---------- Test 1: hello ----------");
            String result1 = helloService.hello("Leo");
            log.info("Result: {}", result1);

            log.info("---------- Test 2: getUserById ----------");
            User user = helloService.getUserById(1001L);
            log.info("Result: {}", user);

            log.info("---------- Test 3: greet ----------");
            User testUser = User.builder()
                    .id(2001L)
                    .name("张三")
                    .age(30)
                    .email("zhangsan@example.com")
                    .build();
            String result3 = helloService.greet("你好", testUser);
            log.info("Result: {}", result3);

            log.info("---------- Test 4: batch call ----------");
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                String result = helloService.hello("BatchUser-" + i);
                log.info("Batch [{}]: {}", i, result);
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("10 calls took {}ms, average {}ms/call", elapsed, elapsed / 10);
        } catch (Exception e) {
            log.error("RPC call failed", e);
        } finally {
            nettyClient.shutdown();
        }

        log.info("========================================");
        log.info("RPC client finished");
        log.info("========================================");
    }

    private static void applyCommandLineOverrides(RpcConfig rpcConfig, String[] args) {
        if (args.length >= 1 && !args[0].trim().isEmpty()) {
            rpcConfig.setNacosAddress(args[0].trim());
        }
        if (args.length >= 2 && !args[1].trim().isEmpty()) {
            rpcConfig.setRequestTimeout(Long.parseLong(args[1].trim()));
        }
        if (args.length >= 3 && !args[2].trim().isEmpty()) {
            rpcConfig.setLoadBalance(args[2].trim());
        }
    }

    private static LoadBalancer createLoadBalancer(String strategy) {
        String normalized = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        if ("round_robin".equals(normalized) || "round-robin".equals(normalized) || "roundrobin".equals(normalized) || "rr".equals(normalized)) {
            return new RoundRobinLoadBalancer();
        }
        return new RandomLoadBalancer();
    }
}
