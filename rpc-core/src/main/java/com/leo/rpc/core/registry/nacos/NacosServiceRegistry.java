package com.leo.rpc.core.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.core.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * 基于Nacos的服务注册实现
 */
public class NacosServiceRegistry implements ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(NacosServiceRegistry.class);

    private final NamingService namingService;

    public NacosServiceRegistry(String nacosAddress) {
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr", nacosAddress);
            this.namingService = NamingFactory.createNamingService(properties);
            log.info("Nacos服务注册中心连接成功: {}", nacosAddress);
        } catch (NacosException e) {
            throw new RpcException("连接Nacos注册中心失败: " + nacosAddress, e);
        }
    }

    @Override
    public void registerService(String serviceName, InetSocketAddress serviceAddress) {
        try {
            namingService.registerInstance(serviceName, serviceAddress.getHostString(), serviceAddress.getPort());
            log.info("服务注册成功: {} -> {}:{}", serviceName, serviceAddress.getHostString(), serviceAddress.getPort());
        } catch (NacosException e) {
            throw new RpcException("注册服务失败: " + serviceName, e);
        }
    }

    @Override
    public void deregisterService(String serviceName, InetSocketAddress serviceAddress) {
        try {
            namingService.deregisterInstance(serviceName, serviceAddress.getHostString(), serviceAddress.getPort());
            log.info("服务注销成功: {} -> {}:{}", serviceName, serviceAddress.getHostString(), serviceAddress.getPort());
        } catch (NacosException e) {
            throw new RpcException("注销服务失败: " + serviceName, e);
        }
    }

    /**
     * 获取Nacos NamingService（供ServiceDiscovery复用）
     */
    public NamingService getNamingService() {
        return namingService;
    }
}
