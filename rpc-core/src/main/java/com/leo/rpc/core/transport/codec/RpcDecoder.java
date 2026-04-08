package com.leo.rpc.core.transport.codec;

import com.leo.rpc.common.enums.RpcResponseCode;
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
 * RPC decoder.
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(RpcDecoder.class);

    private final Serializer serializer;

    public RpcDecoder(Serializer serializer) {
        super(RpcProtocolConstants.MAX_FRAME_LENGTH, 13, 4, 0, 0);
        this.serializer = serializer;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
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
        if (frame.readableBytes() < RpcProtocolConstants.HEADER_LENGTH) {
            throw new RpcException("Frame length is too short");
        }

        short magic = frame.readShort();
        if (magic != RpcProtocolConstants.MAGIC) {
            throw new RpcException("Invalid magic number: " + Integer.toHexString(magic));
        }

        byte version = frame.readByte();
        if (version != RpcProtocolConstants.VERSION) {
            throw new RpcException("Unsupported protocol version: " + version);
        }

        byte msgType = frame.readByte();
        byte status = frame.readByte();
        long requestIdHash = frame.readLong();
        int dataLength = frame.readInt();

        if (dataLength <= 0) {
            throw new RpcException("Invalid data length: " + dataLength);
        }
        if (frame.readableBytes() < dataLength) {
            throw new RpcException("Body length is insufficient: expected=" + dataLength + ", actual=" + frame.readableBytes());
        }

        byte[] data = new byte[dataLength];
        frame.readBytes(data);

        if (msgType == RpcProtocolConstants.MSG_TYPE_REQUEST
                || msgType == RpcProtocolConstants.MSG_TYPE_HEARTBEAT_REQUEST) {
            RpcRequest request = serializer.deserialize(data, RpcRequest.class);
            validateRequest(request, requestIdHash, msgType, status);
            log.debug("Decoded message: type={}, length={}", RpcRequest.class.getSimpleName(), dataLength);
            return request;
        }

        if (msgType == RpcProtocolConstants.MSG_TYPE_RESPONSE
                || msgType == RpcProtocolConstants.MSG_TYPE_HEARTBEAT_RESPONSE) {
            RpcResponse<?> response = serializer.deserialize(data, RpcResponse.class);
            validateResponse(response, requestIdHash, msgType, status);
            log.debug("Decoded message: type={}, length={}", RpcResponse.class.getSimpleName(), dataLength);
            return response;
        }

        throw new RpcException("Unsupported message type: " + msgType);
    }

    private void validateRequest(RpcRequest request, long requestIdHash, byte msgType, byte status) {
        if (request == null || request.getRequestId() == null) {
            throw new RpcException("Request body is empty");
        }
        if (request.getRequestId().hashCode() != (int) requestIdHash) {
            throw new RpcException("Request id validation failed");
        }

        boolean heartbeat = msgType == RpcProtocolConstants.MSG_TYPE_HEARTBEAT_REQUEST;
        if (heartbeat != RpcProtocolConstants.isHeartbeatRequest(request)) {
            throw new RpcException("Request type does not match body");
        }

        byte expectedStatus = heartbeat ? (byte) RpcResponseCode.HEARTBEAT_REQUEST.getCode() : 0;
        if (status != expectedStatus) {
            throw new RpcException("Request status validation failed");
        }
    }

    private void validateResponse(RpcResponse<?> response, long requestIdHash, byte msgType, byte status) {
        if (response == null || response.getRequestId() == null) {
            throw new RpcException("Response body is empty");
        }
        if (response.getRequestId().hashCode() != (int) requestIdHash) {
            throw new RpcException("Response id validation failed");
        }
        if (response.getCode() == null) {
            throw new RpcException("Response code is empty");
        }
        if (status != response.getCode().byteValue()) {
            throw new RpcException("Response status does not match body");
        }

        boolean heartbeat = msgType == RpcProtocolConstants.MSG_TYPE_HEARTBEAT_RESPONSE;
        if (heartbeat != RpcProtocolConstants.isHeartbeatResponse(response)) {
            throw new RpcException("Response type does not match body");
        }
    }
}
