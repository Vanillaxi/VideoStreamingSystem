package com.video.filter;

import com.video.entity.User;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil; // 确保导入的是 RedisUtil
import com.video.utils.UserHolder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
@WebFilter("/*")
public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 1. 获取 Token
        String token = req.getHeader("Authorization");

        // 2. 逻辑分流
        if (token != null && !token.isEmpty()) {
            // 尝试从 Redis 获取用户信息
            String key = "login:token:" + token;
            String userJson = RedisUtil.get(key);

        
            if (userJson != null) {
                // 3. 刷新有效期
                RedisUtil.expire(key, 1800);

                // 4. 保存到 ThreadLocal 供后续 Service 使用
                User user = JSONUtil.toBean(userJson, User.class);
                UserHolder.saveUser(user);
            }
        }

        // 5. 鉴权判定 - 换成 getRequestURI 拿完整路径
        String path = req.getRequestURI();
        log.info("Filter正在校验路径: {}", path);

        // 白名单放行逻辑改进
        if (path.contains("/login") || path.contains("/register")) {
            log.info("白名单匹配成功，放行: {}", path);
            chain.doFilter(request, response);
            return;
        }
        // 校验是否登录成功（ThreadLocal 中是否有值）
        if (UserHolder.getUser() == null) {
            log.warn("拦截到未登录请求: {}", path);
            resp.setStatus(401);
            resp.getWriter().write("Please login first");
            return;
        }

        try {
            // 已登录，执行后续逻辑
            chain.doFilter(request, response);
        } finally {
            // 6. 释放资源，防止内存泄漏
            UserHolder.removeUser();
        }
    }
}