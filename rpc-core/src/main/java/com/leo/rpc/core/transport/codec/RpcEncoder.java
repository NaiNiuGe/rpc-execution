package com.leo.rpc.core.transport.codec;

import com.leo.rpc.common.enums.RpcResponseCode;
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

        byte msgType;
        byte status;
        String requestId;

        if (msg instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) msg;
            if (RpcProtocolConstants.isHeartbeatRequest(request)) {
                msgType = RpcProtocolConstants.MSG_TYPE_HEARTBEAT_REQUEST;
                status = (byte) RpcResponseCode.HEARTBEAT_REQUEST.getCode();
            } else {
                msgType = RpcProtocolConstants.MSG_TYPE_REQUEST;
                status = 0;
            }
            requestId = request.getRequestId();
        } else if (msg instanceof RpcResponse) {
            RpcResponse<?> response = (RpcResponse<?>) msg;
            if (RpcProtocolConstants.isHeartbeatResponse(response)) {
                msgType = RpcProtocolConstants.MSG_TYPE_HEARTBEAT_RESPONSE;
            } else {
                msgType = RpcProtocolConstants.MSG_TYPE_RESPONSE;
            }
            status = response.getCode() == null ? 0 : response.getCode().byteValue();
            requestId = response.getRequestId();
        } else {
            throw new IllegalArgumentException("不支持的消息类型: " + msg.getClass());
        }

        out.writeShort(RpcProtocolConstants.MAGIC);
        out.writeByte(RpcProtocolConstants.VERSION);
        out.writeByte(msgType);
        out.writeByte(status);
        out.writeLong(requestId != null ? requestId.hashCode() : 0L);

        byte[] data = serializer.serialize(msg);
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
