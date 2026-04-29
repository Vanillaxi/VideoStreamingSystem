package com.video.proxy;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
public class ServiceProxy implements InvocationHandler {
    private Object target; //被代理的真实对象
    public ServiceProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            log.info("【AOP 日志】开始执行方法: {}", method.getName());
            Object result = method.invoke(target, args);
            log.info("【AOP 日志】方法执行结束: {}", method.getName());
            return result;
        } catch (Exception e) {
            //反射调用会把异常包在 InvocationTargetException 里，需要取出来
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("【AOP 异常捕捉】方法 {} 出错: {}", method.getName(), cause.getMessage(), cause);
            throw cause; // 继续向上抛，给 Controller 捕获
        }
    }

    // 封装创建代理
    public static Object createProxy(Object target) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),//类加载器
                target.getClass().getInterfaces(),//目标接口
                new ServiceProxy(target)
        );
    }
}
