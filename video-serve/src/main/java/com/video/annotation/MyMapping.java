package com.video.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//待会实现路由自动映射
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyMapping {
    String value(); // 对应的路径，如 "/like"
    String method() default "GET"; // 请求方式
}