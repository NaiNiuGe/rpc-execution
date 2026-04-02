package com.leo.rpc.core.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalancer {

    /**
     * 从服务实例列表中选择一个实例
     *
     * @param instances 服务实例列表
     * @return 选中的实例
     */
    Instance select(List<Instance> instances);
}
