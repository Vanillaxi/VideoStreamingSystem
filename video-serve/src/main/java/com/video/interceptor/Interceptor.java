package com.video.interceptor;

import java.lang.reflect.Method;

public interface Interceptor {
    boolean before(Object target, Method method, Object[] args);
}