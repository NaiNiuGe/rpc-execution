package com.leo.rpc.core.transport.client;

import com.leo.rpc.common.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 未处理的RPC请求管理器
 */
public class UnprocessedRequests {

    private static final Logger log = LoggerFactory.getLogger(UnprocessedRequests.class);

    private final Map<String, CompletableFuture<RpcResponse<?>>> unprocessedResponses =
            new ConcurrentHashMap<>();

    /**
     * 注册新的请求
     */
    public void put(String requestId, CompletableFuture<RpcResponse<?>> future) {
        unprocessedResponses.put(requestId, future);
    }

    /**
     * 完成请求
     */
    public void complete(RpcResponse<?> response) {
        if (response == null || response.getRequestId() == null) {
            log.warn("忽略空响应");
            return;
        }

        CompletableFuture<RpcResponse<?>> future = remove(response.getRequestId());
        if (future == null) {
            log.debug("忽略过期或重复响应: requestId={}", response.getRequestId());
            return;
        }
        future.complete(response);
    }

    /**
     * 移除请求
     */
    public CompletableFuture<RpcResponse<?>> remove(String requestId) {
        return unprocessedResponses.remove(requestId);
    }
}
