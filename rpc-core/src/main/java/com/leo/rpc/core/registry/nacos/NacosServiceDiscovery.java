package com.leo.rpc.core.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.core.loadbalance.LoadBalancer;
import com.leo.rpc.core.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

/**
 * 基于Nacos的服务发现实现
 */
public class NacosServiceDiscovery implements ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(NacosServiceDiscovery.class);

    private final NamingService namingService;
    private final LoadBalancer loadBalancer;

    public NacosServiceDiscovery(String nacosAddress, LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr", nacosAddress);
            this.namingService = NamingFactory.createNamingService(properties);
            log.info("Nacos服务发现连接成功: {}", nacosAddress);
        } catch (NacosException e) {
            throw new RpcException("连接Nacos注册中心失败: " + nacosAddress, e);
        }
    }

    @Override
    public InetSocketAddress discoverService(String serviceName) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName, true);
            if (instances == null || instances.isEmpty()) {
                throw new RpcException("未找到可用的服务实例: " + serviceName);
            }

            // 使用负载均衡策略选择一个实例
            Instance selectedInstance = loadBalancer.select(instances);
            log.debug("服务发现成功: {} -> {}:{}", serviceName, selectedInstance.getIp(), selectedInstance.getPort());
            return new InetSocketAddress(selectedInstance.getIp(), selectedInstance.getPort());
        } catch (NacosException e) {
            throw new RpcException("发现服务失败: " + serviceName, e);
        }
    }
}
