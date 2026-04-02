package com.leo.rpc.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RPC框架配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcConfig {

    /**
     * Nacos服务器地址，默认localhost:8848
     */
    @Builder.Default
    private String nacosAddress = "127.0.0.1:8848";

    /**
     * Nacos命名空间
     */
    @Builder.Default
    private String nacosNamespace = "public";

    /**
     * 服务端监听端口
     */
    @Builder.Default
    private int serverPort = 9090;

    /**
     * 服务端主机地址
     */
    @Builder.Default
    private String serverHost = "127.0.0.1";

    /**
     * 序列化方式
     */
    @Builder.Default
    private String serializer = "kryo";

    /**
     * 负载均衡策略
     */
    @Builder.Default
    private String loadBalance = "random";

    /**
     * Netty Boss线程数
     */
    @Builder.Default
    private int bossThreads = 1;

    /**
     * Netty Worker线程数
     */
    @Builder.Default
    private int workerThreads = 0; // 0 = 默认(CPU * 2)

    /**
     * 请求超时时间(毫秒)
     */
    @Builder.Default
    private long requestTimeout = 5000;
}
