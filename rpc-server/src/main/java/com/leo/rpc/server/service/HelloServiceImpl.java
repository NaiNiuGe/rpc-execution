package com.leo.rpc.server.service;

import com.leo.rpc.api.HelloService;
import com.leo.rpc.api.model.User;
import com.leo.rpc.core.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HelloService实现（示例）
 */
@RpcService
public class HelloServiceImpl implements HelloService {

    private static final Logger log = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(String name) {
        log.info("收到hello请求: name={}", name);
        return "Hello, " + name + "! 来自RPC Execution框架";
    }

    @Override
    public User getUserById(Long id) {
        log.info("收到getUserById请求: id={}", id);
        // 模拟返回用户数据
        return User.builder()
                .id(id)
                .name("用户" + id)
                .age(25)
                .email("user" + id + "@example.com")
                .build();
    }

    @Override
    public String greet(String greeting, User user) {
        log.info("收到greet请求: greeting={}, user={}", greeting, user);
        return greeting + ", " + user.getName() + "! 你今年" + user.getAge() + "岁了";
    }
}
