package com.simplerpc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC引用注解，用于标记需要注入的服务引用
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    /**
     * 服务版本号
     */
    String version() default "1.0.0";

    /**
     * 超时时间，单位毫秒
     */
    long timeout() default 5000;
}
