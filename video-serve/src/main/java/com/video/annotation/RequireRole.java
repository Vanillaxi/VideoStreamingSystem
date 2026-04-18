package com.video.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    int value() default 0; // 0:普通用户, 1:会员,2:管理员，3:香草
}