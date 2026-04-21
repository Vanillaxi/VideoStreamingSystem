package com.video.annotation;

import java.lang.annotation.*;


//实现自动注入
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {}