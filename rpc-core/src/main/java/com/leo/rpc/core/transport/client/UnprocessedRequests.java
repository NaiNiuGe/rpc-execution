package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.model.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 未处理的RPC请求管理器
 * <p>
 * 使用CompletableFuture实现异步RPC调用，每个请求对应一个Future，
 * 当收到响应时complete对应的Future。
 */
public class UnprocessedRequests {

    private static final Map<String, CompletableFuture<RpcResponse<?>>> UNPROCESSED_RESPONSES =
            new ConcurrentHashMap<>();

    /**
     * 注册一个新的请求
     */
    public void put(String requestId, CompletableFuture<RpcResponse<?>> future) {
        UNPROCESSED_RESPONSES.put(requestId, future);
    }

    /**
     * 完成一个请求
     */
    public void complete(RpcResponse<?> response) {
        CompletableFuture<RpcResponse<?>> future = UNPROCESSED_RESPONSES.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
        } else {
            throw new IllegalStateException("未找到对应的请求: " + response.getRequestId());
        }
    }

    /**
     * 移除一个请求
     */
    public void remove(String requestId) {
        UNPROCESSED_RESPONSES.remove(requestId);
    }
}
