package com.leo.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * RPC引用注解
 * <p>
 * 标注在字段上，自动注入RPC代理对象
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {

    /**
     * 服务版本号
     */
    String version() default "";

    /**
     * 服务分组
     */
    String group() default "";

    /**
     * 超时时间(毫秒)
     */
    long timeout() default 5000;
}
