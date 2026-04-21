package com.video.annotation;

import java.lang.annotation.*;

//放在Service,Mapper的实现类上面（注入）
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyComponent {}