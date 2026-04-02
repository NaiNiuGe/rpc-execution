package com.leo.rpc.core.provider;

import com.leo.rpc.common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认服务提供者管理实现
 * <p>
 * 使用ConcurrentHashMap存储服务名到服务实例的映射
 */
public class DefaultServiceProvider implements ServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultServiceProvider.class);

    /**
     * 服务映射: 服务名 -> 服务实例
     */
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    @Override
    public void addService(String serviceName, Object serviceImpl) {
        if (serviceMap.containsKey(serviceName)) {
            log.warn("服务已存在，将被覆盖: {}", serviceName);
        }
        serviceMap.put(serviceName, serviceImpl);
        log.info("注册本地服务: {} -> {}", serviceName, serviceImpl.getClass().getName());
    }

    @Override
    public Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if (service == null) {
            throw new RpcException("未找到服务: " + serviceName);
        }
        return service;
    }
}
