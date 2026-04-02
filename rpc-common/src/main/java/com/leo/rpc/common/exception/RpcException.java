package com.leo.rpc.common.exception;

/**
 * RPC异常
 */
public class RpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
