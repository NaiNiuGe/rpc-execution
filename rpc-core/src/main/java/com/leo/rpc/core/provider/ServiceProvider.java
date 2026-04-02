package com.leo.rpc.core.provider;

/**
 * 服务提供者管理接口
 * <p>
 * 管理本地服务实例的映射关系
 */
public interface ServiceProvider {

    /**
     * 注册服务（存储到本地Map）
     *
     * @param serviceName 服务名称
     * @param serviceImpl 服务实现对象
     */
    void addService(String serviceName, Object serviceImpl);

    /**
     * 获取服务实现
     *
     * @param serviceName 服务名称
     * @return 服务实现对象
     */
    Object getService(String serviceName);
}
