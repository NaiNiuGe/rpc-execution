package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty客户端处理器
 * <p>
 * 接收服务端返回的RpcResponse，并通过UnprocessedRequests完成对应的CompletableFuture
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse<?>> {

    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);

    private final UnprocessedRequests unprocessedRequests;

    public NettyClientHandler(UnprocessedRequests unprocessedRequests) {
        this.unprocessedRequests = unprocessedRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse<?> response) throws Exception {
        log.debug("客户端收到响应: requestId={}, code={}", response.getRequestId(), response.getCode());
        unprocessedRequests.complete(response);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.debug("发送心跳到服务端: {}", ctx.channel().remoteAddress());
            // 可以发送心跳包，这里简单关闭
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
