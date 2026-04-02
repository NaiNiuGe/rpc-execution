package com.leo.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * RPC服务注解
 * <p>
 * 标注在服务实现类上，表示该类提供RPC服务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {

    /**
     * 服务版本号, 默认空
     */
    String version() default "";

    /**
     * 服务分组, 默认空
     */
    String group() default "";
}
