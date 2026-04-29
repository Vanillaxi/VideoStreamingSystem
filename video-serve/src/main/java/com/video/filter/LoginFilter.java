package com.video.filter;

import com.video.pojo.entity.User;
import com.video.utils.AuthHeaderUtil;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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

        try {
            String token = AuthHeaderUtil.extractBearerToken(req.getHeader("Authorization"));

            if (token != null && !token.isEmpty()) {
                String key = "login:token:" + token;
                String userJson = RedisUtil.get(key);

                if (userJson != null) {
                    RedisUtil.expire(key, 3600);
                    User user = JSONUtil.toBean(userJson, User.class);
                    UserHolder.saveUser(user);
                }

            }

            String path = req.getRequestURI();
            log.info("Filter正在校验路径: {}", path);

            String contextPath = req.getContextPath();
            String servletPath = path;
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                servletPath = path.substring(contextPath.length());
            }

            if (servletPath.equals("/ws") || servletPath.startsWith("/ws/")) {
                log.info("WebSocket 握手路径已放行: {}", path);
                chain.doFilter(request, response);
                return;
            }

            if (path.contains("/login") || path.contains("/register")) {
                log.info("放行: {}", path);
                chain.doFilter(request, response);
                return;
            }

            if (UserHolder.getUser() == null) {
                log.warn("拦截到未登录请求: {}", path);
                resp.setStatus(401);
                resp.getWriter().write("Please login first");
                return;
            }

            chain.doFilter(request, response);
        } finally {
            UserHolder.removeUser();
        }
    }
}
