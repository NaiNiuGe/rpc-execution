package com.leo.rpc.core.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡策略
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Instance select(List<Instance> instances) {
        int index = counter.getAndIncrement() % instances.size();
        // 防止counter溢出
        if (counter.get() > Integer.MAX_VALUE - 1000) {
            counter.set(0);
        }
        return instances.get(index);
    }
}
