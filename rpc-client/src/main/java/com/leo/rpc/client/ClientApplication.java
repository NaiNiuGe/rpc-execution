package com.leo.rpc.client;

import com.leo.rpc.api.HelloService;
import com.leo.rpc.api.model.User;
import com.leo.rpc.core.loadbalance.RandomLoadBalancer;
import com.leo.rpc.core.proxy.RpcClientProxy;
import com.leo.rpc.core.registry.nacos.NacosServiceDiscovery;
import com.leo.rpc.core.transport.client.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC客户端启动类（示例）
 * <p>
 * 使用步骤：
 * 1. 确保Nacos已启动 (默认地址 127.0.0.1:8848)
 * 2. 确保RPC服务端已启动
 * 3. 运行此类进行RPC调用
 */
public class ClientApplication {

    private static final Logger log = LoggerFactory.getLogger(ClientApplication.class);

    public static void main(String[] args) {
        String nacosAddress = "127.0.0.1:8848";
        if (args.length >= 1) {
            nacosAddress = args[0];
        }

        log.info("========================================");
        log.info("  RPC Execution - 客户端启动中...");
        log.info("  Nacos: {}", nacosAddress);
        log.info("========================================");

        // 1. 创建服务发现（使用随机负载均衡）
        NacosServiceDiscovery serviceDiscovery = new NacosServiceDiscovery(nacosAddress, new RandomLoadBalancer());

        // 2. 创建Netty客户端
        NettyClient nettyClient = new NettyClient(serviceDiscovery);

        // 3. 创建RPC代理
        RpcClientProxy proxy = new RpcClientProxy(nettyClient);

        // 4. 获取远程服务代理对象
        HelloService helloService = proxy.getProxy(HelloService.class);

        try {
            // ====== 测试1: 简单字符串调用 ======
            log.info("---------- 测试1: hello ----------");
            String result1 = helloService.hello("Leo");
            log.info("结果: {}", result1);

            // ====== 测试2: 复杂对象返回 ======
            log.info("---------- 测试2: getUserById ----------");
            User user = helloService.getUserById(1001L);
            log.info("结果: {}", user);

            // ====== 测试3: 多参数调用 ======
            log.info("---------- 测试3: greet ----------");
            User testUser = User.builder()
                    .id(2001L)
                    .name("张三")
                    .age(30)
                    .email("zhangsan@example.com")
                    .build();
            String result3 = helloService.greet("你好", testUser);
            log.info("结果: {}", result3);

            // ====== 测试4: 多次调用验证连接复用 ======
            log.info("---------- 测试4: 批量调用测试 ----------");
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                String result = helloService.hello("BatchUser-" + i);
                log.info("批量调用 [{}]: {}", i, result);
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("10次调用耗时: {}ms, 平均: {}ms/次", elapsed, elapsed / 10);

        } catch (Exception e) {
            log.error("RPC调用失败", e);
        } finally {
            // 5. 关闭客户端
            nettyClient.shutdown();
        }

        log.info("========================================");
        log.info("  RPC客户端测试完成");
        log.info("========================================");
    }
}
