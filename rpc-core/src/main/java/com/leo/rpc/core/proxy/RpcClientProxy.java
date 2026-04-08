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
import java.util.concurrent.TimeoutException;

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
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        log.debug("RPC call: {}.{}", method.getDeclaringClass().getName(), method.getName());

        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .params(args)
                .version(version)
                .group(group)
                .build();

        CompletableFuture<RpcResponse<?>> responseFuture = nettyClient.sendRequest(rpcRequest);

        try {
            RpcResponse<?> response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            checkResponse(rpcRequest, response);
            return response.getData();
        } catch (TimeoutException e) {
            responseFuture.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            responseFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (!responseFuture.isDone()) {
                responseFuture.cancel(true);
            }
        }
    }

    private void checkResponse(RpcRequest request, RpcResponse<?> response) {
        if (response == null) {
            throw new RpcException("RPC call failed: response is null, requestId=" + request.getRequestId());
        }
        if (!request.getRequestId().equals(response.getRequestId())) {
            throw new RpcException("RPC call failed: requestId mismatch, expected="
                    + request.getRequestId() + ", actual=" + response.getRequestId());
        }
        if (response.getCode() == null || !response.getCode().equals(RpcResponseCode.SUCCESS.getCode())) {
            throw new RpcException("RPC call failed: " + response.getMessage()
                    + ", requestId=" + request.getRequestId());
        }
    }
}
