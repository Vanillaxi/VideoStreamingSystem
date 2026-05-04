package com.video.filter;

import com.video.pojo.dto.Result;
import com.video.utils.JSONUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

//全局异常处理器
public class GlobalExceptionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            //Controlelr报错（空指针，数据库断开）
            e.printStackTrace();
            response.getWriter().write(JSONUtil.toJson(Result.error("服务器内部错误，请联系香草管理员")));
        }
    }
}
