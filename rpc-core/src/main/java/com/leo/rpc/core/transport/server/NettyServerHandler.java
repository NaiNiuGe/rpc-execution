package com.leo.rpc.core.transport.server;

import com.leo.rpc.common.enums.RpcResponseCode;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.provider.ServiceProvider;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Netty服务端处理器
 * <p>
 * 接收客户端的RpcRequest，通过反射调用本地服务实现，返回RpcResponse
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    private final ServiceProvider serviceProvider;

    public NettyServerHandler(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.debug("服务端收到请求: requestId={}, service={}, method={}",
                request.getRequestId(), request.getInterfaceName(), request.getMethodName());

        RpcResponse<?> response;
        try {
            // 获取服务实现
            Object service = serviceProvider.getService(request.getServiceName());
            // 反射调用方法
            Object result = invokeMethod(service, request);
            // 构建成功响应
            response = RpcResponse.success(result, request.getRequestId());
        } catch (Exception e) {
            log.error("服务调用异常: requestId={}", request.getRequestId(), e);
            response = RpcResponse.fail(e.getMessage());
            response.setRequestId(request.getRequestId());
        }

        // 写回响应
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    /**
     * 通过反射调用目标方法
     */
    private Object invokeMethod(Object service, RpcRequest request)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = service.getClass().getMethod(request.getMethodName(), request.getParamTypes());
        method.setAccessible(true);
        return method.invoke(service, request.getParams());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务端异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开: {}", ctx.channel().remoteAddress());
    }
}
