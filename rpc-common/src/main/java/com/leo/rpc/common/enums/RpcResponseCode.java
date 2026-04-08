package com.leo.rpc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RPC响应状态码
 */
@Getter
@AllArgsConstructor
public enum RpcResponseCode {

    SUCCESS(200, "调用成功"),
    FAIL(500, "调用失败"),
    NOT_FOUND_SERVICE(404, "未找到指定服务"),
    NOT_FOUND_METHOD(405, "未找到指定方法"),
    HEARTBEAT_REQUEST(100, "心跳请求"),
    HEARTBEAT_RESPONSE(101, "心跳响应");

    private final int code;
    private final String message;
}
