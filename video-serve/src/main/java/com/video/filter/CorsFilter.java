package com.video.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class CorsFilter implements Filter {
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://172.20.10.9:5173"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        String requestMethod = req.getHeader("Access-Control-Request-Method");
        String requestHeaders = req.getHeader("Access-Control-Request-Headers");
        boolean allowedOrigin = origin != null && ALLOWED_ORIGINS.contains(origin);

        if (allowedOrigin) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, token, Authorization");
            resp.setHeader("Access-Control-Max-Age", "3600");
            resp.setHeader("Vary", "Origin");
        }

        log.info("CORS request method={}, uri={}, origin={}, accessControlRequestMethod={}, "
                        + "accessControlRequestHeaders={}, allowedOrigin={}, allowOriginHeader={}",
                req.getMethod(),
                req.getRequestURI(),
                origin,
                requestMethod,
                requestHeaders,
                allowedOrigin,
                resp.getHeader("Access-Control-Allow-Origin"));

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            log.info("CORS preflight handled, status=200, uri={}", req.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}
