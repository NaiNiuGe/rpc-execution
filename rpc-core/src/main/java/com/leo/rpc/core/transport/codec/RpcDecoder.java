package com.leo.rpc.core.transport.codec;

import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC解码器
 * <p>
 * 继承LengthFieldBasedFrameDecoder，基于长度字段的帧解码器，解决TCP粘包/拆包问题。
 * 然后根据协议头解析消息体，使用Kryo反序列化。
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(RpcDecoder.class);

    private final Serializer serializer;

    /**
     * 构造方法
     *
     * @param serializer 序列化器
     */
    public RpcDecoder(Serializer serializer) {
        // maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip
        // 长度字段偏移量 = magic(2) + version(1) + msgType(1) + status(1) + requestId(8) = 13
        // 长度字段自身长度 = 4
        // 不跳过任何字节，在decode方法中手动读取协议头
        super(RpcProtocolConstants.MAX_FRAME_LENGTH, 13, 4, 0, 0);
        this.serializer = serializer;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 先由父类根据长度字段解码出完整的帧
        Object decoded = super.decode(ctx, in);
        if (decoded == null) {
            return null;
        }

        ByteBuf frame = (ByteBuf) decoded;
        try {
            return decodeFrame(frame);
        } finally {
            frame.release();
        }
    }

    private Object decodeFrame(ByteBuf frame) {
        // 检查可读字节是否足够
        if (frame.readableBytes() < RpcProtocolConstants.HEADER_LENGTH) {
            throw new RpcException("帧长度不足，无法解析协议头");
        }

        // 读取魔数
        short magic = frame.readShort();
        if (magic != RpcProtocolConstants.MAGIC) {
            throw new RpcException("无效的魔数: " + Integer.toHexString(magic));
        }

        // 读取版本号
        byte version = frame.readByte();
        if (version != RpcProtocolConstants.VERSION) {
            throw new RpcException("不支持的协议版本: " + version);
        }

        // 读取消息类型
        byte msgType = frame.readByte();

        // 读取状态位
        byte status = frame.readByte();

        // 读取请求ID
        long requestId = frame.readLong();

        // 读取数据长度
        int dataLength = frame.readInt();

        if (dataLength <= 0) {
            throw new RpcException("无效的数据长度: " + dataLength);
        }

        // 读取数据
        byte[] data = new byte[dataLength];
        frame.readBytes(data);

        // 根据消息类型反序列化
        Class<?> targetClass;
        if (msgType == RpcProtocolConstants.MSG_TYPE_REQUEST) {
            targetClass = RpcRequest.class;
        } else if (msgType == RpcProtocolConstants.MSG_TYPE_RESPONSE) {
            targetClass = RpcResponse.class;
        } else {
            throw new RpcException("不支持的消息类型: " + msgType);
        }

        Object result = serializer.deserialize(data, targetClass);
        log.debug("解码消息: type={}, length={}", targetClass.getSimpleName(), dataLength);
        return result;
    }
}
