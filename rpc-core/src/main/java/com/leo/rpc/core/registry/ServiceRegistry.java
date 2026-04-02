package com.leo.rpc.core.registry;

import java.net.InetSocketAddress;

/**
 * 服务注册接口
 */
public interface ServiceRegistry {

    /**
     * 注册服务到注册中心
     *
     * @param serviceName    服务名称
     * @param serviceAddress 服务地址
     */
    void registerService(String serviceName, InetSocketAddress serviceAddress);

    /**
     * 注销服务
     *
     * @param serviceName    服务名称
     * @param serviceAddress 服务地址
     */
    void deregisterService(String serviceName, InetSocketAddress serviceAddress);
}
