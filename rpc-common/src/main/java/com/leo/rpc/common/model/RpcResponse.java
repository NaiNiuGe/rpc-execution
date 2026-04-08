package com.leo.rpc.common.model;

import com.leo.rpc.common.enums.RpcResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     */
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestId(requestId);
        response.setCode(RpcResponseCode.SUCCESS.getCode());
        response.setMessage(RpcResponseCode.SUCCESS.getMessage());
        response.setData(data);
        return response;
    }

    /**
     * 失败响应
     */
    public static <T> RpcResponse<T> fail(String message) {
        return fail(RpcResponseCode.FAIL, message, null);
    }

    /**
     * 失败响应
     */
    public static <T> RpcResponse<T> fail(RpcResponseCode responseCode, String message, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestId(requestId);
        response.setCode(responseCode.getCode());
        response.setMessage(message == null ? responseCode.getMessage() : message);
        return response;
    }

    /**
     * 失败响应
     */
    public static <T> RpcResponse<T> fail(RpcResponseCode responseCode, String requestId) {
        return fail(responseCode, responseCode.getMessage(), requestId);
    }

    /**
     * 心跳响应
     */
    public static <T> RpcResponse<T> heartbeatResponse(String requestId) {
        return fail(RpcResponseCode.HEARTBEAT_RESPONSE, requestId);
    }
}
