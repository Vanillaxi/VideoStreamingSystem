package com.video.interceptor;

import com.video.annotation.RequireRole;
import com.video.pojo.entity.User;
import com.video.utils.UserHolder;

import java.lang.reflect.Method;

public class RoleInterceptor implements Interceptor {
    @Override
    public boolean before(Object target, Method method, Object[] args) {
        if (method.isAnnotationPresent(RequireRole.class)) {
            int need = method.getAnnotation(RequireRole.class).value();
            User user = UserHolder.getUser();

            //拦截
            if (user == null || user.getRole() != need) {
                return false;
            }
        }
        return true;
    }

}
