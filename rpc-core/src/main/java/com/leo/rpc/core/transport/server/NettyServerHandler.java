package com.leo.rpc.core.transport.server;

import com.leo.rpc.common.enums.RpcResponseCode;
import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.provider.ServiceProvider;
import com.leo.rpc.core.transport.codec.RpcProtocolConstants;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Netty server handler.
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    private final ServiceProvider serviceProvider;

    public NettyServerHandler(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.debug("Received request: requestId={}, service={}, method={}",
                request.getRequestId(), request.getInterfaceName(), request.getMethodName());

        if (RpcProtocolConstants.isHeartbeatRequest(request)) {
            ctx.writeAndFlush(RpcResponse.heartbeatResponse(request.getRequestId()))
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            return;
        }

        RpcResponse<?> response;
        try {
            Object service = serviceProvider.getService(request.getServiceName());
            Object result = invokeMethod(service, request);
            response = RpcResponse.success(result, request.getRequestId());
        } catch (RpcException e) {
            response = RpcResponse.fail(RpcResponseCode.NOT_FOUND_SERVICE, resolveMessage(e), request.getRequestId());
        } catch (ClassNotFoundException e) {
            response = RpcResponse.fail(RpcResponseCode.NOT_FOUND_SERVICE, resolveMessage(e), request.getRequestId());
        } catch (NoSuchMethodException e) {
            response = RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD, resolveMessage(e), request.getRequestId());
        } catch (InvocationTargetException e) {
            response = RpcResponse.fail(RpcResponseCode.FAIL, resolveMessage(e), request.getRequestId());
        } catch (IllegalAccessException e) {
            response = RpcResponse.fail(RpcResponseCode.FAIL, resolveMessage(e), request.getRequestId());
        } catch (Exception e) {
            response = RpcResponse.fail(RpcResponseCode.FAIL, resolveMessage(e), request.getRequestId());
        }

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private Object invokeMethod(Object service, RpcRequest request)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> interfaceClass = Class.forName(request.getInterfaceName());
        if (!interfaceClass.isAssignableFrom(service.getClass())) {
            throw new RpcException("Service does not implement interface: " + request.getInterfaceName());
        }

        Method method = interfaceClass.getMethod(request.getMethodName(), request.getParamTypes());
        return method.invoke(service, request.getParams());
    }

    private String resolveMessage(Throwable throwable) {
        Throwable rootCause = throwable;
        if (throwable instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) throwable).getTargetException();
            if (targetException != null) {
                rootCause = targetException;
            }
        }
        String message = rootCause.getMessage();
        return message != null ? message : rootCause.getClass().getSimpleName();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Server error: {}", cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.warn("Server read idle, close connection: {}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
    }
}
