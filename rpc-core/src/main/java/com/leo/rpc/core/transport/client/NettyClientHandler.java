package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty客户端处理器
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse<?>> {

    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);

    private final NettyClient nettyClient;

    public NettyClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse<?> response) throws Exception {
        log.debug("客户端收到响应: requestId={}, code={}", response.getRequestId(), response.getCode());
        nettyClient.completeResponse(ctx.channel(), response);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                log.debug("发送心跳到服务端: {}", ctx.channel().remoteAddress());
                nettyClient.sendHeartbeat(ctx.channel());
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端异常: {}", cause.getMessage(), cause);
        nettyClient.handleChannelClosed(ctx.channel(), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接关闭: {}", ctx.channel().remoteAddress());
        nettyClient.handleChannelClosed(ctx.channel(), null);
        super.channelInactive(ctx);
    }
}
