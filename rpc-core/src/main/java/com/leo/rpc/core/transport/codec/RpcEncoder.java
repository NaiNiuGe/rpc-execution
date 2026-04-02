package com.leo.rpc.core.transport.codec;

import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC编码器
 * <p>
 * 将RpcRequest/RpcResponse序列化为自定义协议格式的字节流
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private static final Logger log = LoggerFactory.getLogger(RpcEncoder.class);

    private final Serializer serializer;

    public RpcEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.debug("编码消息: {}", msg.getClass().getSimpleName());

        // 写入魔数
        out.writeShort(RpcProtocolConstants.MAGIC);
        // 写入版本号
        out.writeByte(RpcProtocolConstants.VERSION);

        // 写入消息类型
        byte msgType;
        if (msg instanceof RpcRequest) {
            msgType = RpcProtocolConstants.MSG_TYPE_REQUEST;
        } else if (msg instanceof RpcResponse) {
            msgType = RpcProtocolConstants.MSG_TYPE_RESPONSE;
        } else {
            throw new IllegalArgumentException("不支持的消息类型: " + msg.getClass());
        }
        out.writeByte(msgType);

        // 写入状态位（预留）
        out.writeByte(0);

        // 写入请求ID
        if (msg instanceof RpcRequest) {
            String requestId = ((RpcRequest) msg).getRequestId();
            out.writeLong(requestId != null ? requestId.hashCode() : 0);
        } else {
            String requestId = ((RpcResponse<?>) msg).getRequestId();
            out.writeLong(requestId != null ? requestId.hashCode() : 0);
        }

        // 序列化消息体
        byte[] data = serializer.serialize(msg);
        // 写入数据长度
        out.writeInt(data.length);
        // 写入数据
        out.writeBytes(data);
    }
}
