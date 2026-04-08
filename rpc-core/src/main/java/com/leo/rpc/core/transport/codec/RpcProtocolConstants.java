package com.leo.rpc.core.transport.codec;

import com.leo.rpc.common.enums.RpcResponseCode;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;

/**
 * 自定义RPC协议常量
 */
public final class RpcProtocolConstants {

    private RpcProtocolConstants() {
    }

    /**
     * 协议魔数
     */
    public static final short MAGIC = (short) 0xCAFE;

    /**
     * 协议版本
     */
    public static final byte VERSION = 1;

    /**
     * 消息类型：请求
     */
    public static final byte MSG_TYPE_REQUEST = 1;

    /**
     * 消息类型：响应
     */
    public static final byte MSG_TYPE_RESPONSE = 2;

    /**
     * 消息类型：心跳请求
     */
    public static final byte MSG_TYPE_HEARTBEAT_REQUEST = 3;

    /**
     * 消息类型：心跳响应
     */
    public static final byte MSG_TYPE_HEARTBEAT_RESPONSE = 4;

    /**
     * 心跳服务接口名
     */
    public static final String HEARTBEAT_INTERFACE_NAME = "__rpc_heartbeat__";

    /**
     * 心跳方法名
     */
    public static final String HEARTBEAT_METHOD_NAME = "__heartbeat__";

    /**
     * 心跳版本
     */
    public static final String HEARTBEAT_VERSION = "";

    /**
     * 心跳分组
     */
    public static final String HEARTBEAT_GROUP = "";

    /**
     * 心跳服务名
     */
    public static final String HEARTBEAT_SERVICE_NAME =
            HEARTBEAT_INTERFACE_NAME + "#" + HEARTBEAT_VERSION + "#" + HEARTBEAT_GROUP;

    /**
     * 请求超时清理时间，单位：毫秒
     */
    public static final long REQUEST_TIMEOUT_MILLIS = 5_000L;

    /**
     * 请求超时清理缓冲时间，单位：毫秒
     */
    public static final long REQUEST_TIMEOUT_CLEANUP_DELAY_MILLIS = 1_000L;

    /**
     * 心跳超时时间，单位：毫秒
     */
    public static final long HEARTBEAT_TIMEOUT_MILLIS = 5_000L;

    /**
     * 协议头长度
     */
    public static final int HEADER_LENGTH = 17;

    /**
     * 最大帧长度
     */
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    public static boolean isHeartbeatRequest(RpcRequest request) {
        return request != null
                && HEARTBEAT_INTERFACE_NAME.equals(request.getInterfaceName())
                && HEARTBEAT_METHOD_NAME.equals(request.getMethodName())
                && isBlank(request.getVersion())
                && isBlank(request.getGroup())
                && request.getParamTypes() != null
                && request.getParamTypes().length == 0
                && request.getParams() != null
                && request.getParams().length == 0;
    }

    public static boolean isHeartbeatResponse(RpcResponse<?> response) {
        return response != null
                && response.getCode() != null
                && response.getCode().intValue() == RpcResponseCode.HEARTBEAT_RESPONSE.getCode();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
