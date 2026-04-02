package com.leo.rpc.api;

import com.leo.rpc.api.model.User;

/**
 * Hello服务接口（示例）
 * <p>
 * 这是一个RPC服务的API定义，客户端和服务端都依赖此接口
 */
public interface HelloService {

    /**
     * 简单字符串方法
     */
    String hello(String name);

    /**
     * 复杂对象传输方法
     */
    User getUserById(Long id);

    /**
     * 带多参数的方法
     */
    String greet(String greeting, User user);
}
