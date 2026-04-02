package com.leo.rpc.core.transport.codec;

/**
 * 自定义RPC协议常量
 * <p>
 * 协议格式:
 * +--------+--------+--------+--------+--------+
 * | Magic  | Version| MsgType| Status | ReqId  |
 * | 2bytes | 1byte  | 1byte  | 1byte  | 8bytes |
 * +--------+--------+--------+--------+--------+
 * | DataLength      | Data (Kryo serialized)    |
 * | 4bytes          | variable                   |
 * +--------+--------+---------------------------+
 */
public class RpcProtocolConstants {

    /**
     * 魔数，用于快速识别RPC协议包
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
     * 协议头长度 = magic(2) + version(1) + msgType(1) + status(1) + requestId(8) + dataLength(4) = 17
     */
    public static final int HEADER_LENGTH = 17;

    /**
     * 最大帧长度 (8MB)
     */
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;
}
