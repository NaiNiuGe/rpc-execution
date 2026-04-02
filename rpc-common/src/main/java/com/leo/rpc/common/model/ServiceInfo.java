package com.leo.rpc.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务信息（注册到Nacos的元数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务主机地址
     */
    private String host;

    /**
     * 服务端口
     */
    private int port;

    /**
     * 版本号
     */
    private String version;

    /**
     * 分组
     */
    private String group;

    /**
     * 权重（用于负载均衡）
     */
    private double weight;

    /**
     * 获取服务地址
     */
    public String getAddress() {
        return host + ":" + port;
    }
}
