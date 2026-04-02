package com.leo.rpc.core.proxy;

import com.leo.rpc.common.enums.RpcResponseCode;
import com.leo.rpc.common.exception.RpcException;
import com.leo.rpc.common.model.RpcRequest;
import com.leo.rpc.common.model.RpcResponse;
import com.leo.rpc.core.transport.client.NettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端动态代理
 * <p>
 * 通过JDK动态代理，将本地方法调用透明转化为远程RPC调用。
 * 使用方只需要调用接口方法，代理会自动完成：
 * 1. 构建RpcRequest
 * 2. 通过NettyClient发送请求
 * 3. 等待响应并返回结果
 */
public class RpcClientProxy implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(RpcClientProxy.class);

    private final NettyClient nettyClient;
    private final String version;
    private final String group;
    private final long timeoutMs;

    public RpcClientProxy(NettyClient nettyClient, String version, String group, long timeoutMs) {
        this.nettyClient = nettyClient;
        this.version = version;
        this.group = group;
        this.timeoutMs = timeoutMs;
    }

    public RpcClientProxy(NettyClient nettyClient) {
        this(nettyClient, "", "", 5000);
    }

    /**
     * 获取代理对象
     *
     * @param serviceClass 服务接口Class
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                this
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 跳过Object类的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        log.debug("RPC调用: {}.{}", method.getDeclaringClass().getName(), method.getName());

        // 构建RPC请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .params(args)
                .version(version)
                .group(group)
                .build();

        // 发送请求（异步）
        CompletableFuture<RpcResponse<?>> responseFuture = nettyClient.sendRequest(rpcRequest);

        // 等待响应（带超时）
        RpcResponse<?> response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

        // 检查响应状态
        checkResponse(rpcRequest, response);

        return response.getData();
    }

    /**
     * 校验RPC响应
     */
    private void checkResponse(RpcRequest request, RpcResponse<?> response) {
        if (response == null) {
            throw new RpcException("RPC调用失败: 响应为空, requestId=" + request.getRequestId());
        }
        if (!request.getRequestId().equals(response.getRequestId())) {
            throw new RpcException("RPC调用失败: 请求ID不匹配, expected="
                    + request.getRequestId() + ", actual=" + response.getRequestId());
        }
        if (response.getCode() == null || response.getCode() != RpcResponseCode.SUCCESS.getCode()) {
            throw new RpcException("RPC调用失败: " + response.getMessage()
                    + ", requestId=" + request.getRequestId());
        }
    }
}
