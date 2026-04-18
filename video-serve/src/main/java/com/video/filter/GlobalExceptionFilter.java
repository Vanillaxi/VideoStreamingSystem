package com.video.filter;

import com.video.pojo.dto.Result;
import com.video.utils.JSONUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class GlobalExceptionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            //Controlelr报错
            e.printStackTrace();
            response.getWriter().write(JSONUtil.toJson(Result.error("系统繁忙，请稍后再试")));
        }
    }
}
