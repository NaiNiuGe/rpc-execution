package com.leo.rpc.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 接口名称
     */
    private String interfaceName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 参数类型列表
     */
    private Class<?>[] paramTypes;

    /**
     * 参数值列表
     */
    private Object[] params;

    /**
     * 版本号（用于服务多版本）
     */
    private String version;

    /**
     * 分组（用于服务分组）
     */
    private String group;

    /**
     * 获取RPC服务名称（接口名+版本+分组）
     */
    public String getServiceName() {
        return this.interfaceName + "#" + this.version + "#" + this.group;
    }
}
