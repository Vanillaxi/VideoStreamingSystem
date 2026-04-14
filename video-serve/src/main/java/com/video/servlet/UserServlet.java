package com.video.servlet;

import com.video.config.BeanFactory;
import com.video.entity.User;
import com.video.result.Result;
import com.video.service.UserService;
import com.video.utils.JSONUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebServlet("/user/*")
public class UserServlet extends HttpServlet {

    private UserService userService = BeanFactory.getBean(UserService.class);


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json;charset=utf-8");


        String json = req.getReader().lines().collect(java.util.stream.Collectors.joining());
        User userFromGrid = JSONUtil.toBean(json, User.class);

        /**
         * 注册
         */
        if ("/register".equals(path)) {
            userFromGrid.setRole(1);
            if (userService.register(userFromGrid)) {
                resp.getWriter().write(JSONUtil.toJson(Result.success("注册成功")));
            } else {
                resp.getWriter().write(JSONUtil.toJson(Result.error("注册失败：用户名已存在或参数错误")));
            }

        }

        /**
         * 登录
         */
        else if ("/login".equals(path)) {
            String token = userService.login(userFromGrid.getUsername(), userFromGrid.getPassword());
            if (token != null) {
                resp.setHeader("Authorization", token);
                resp.getWriter().write(JSONUtil.toJson(Result.success(token)));
            } else {
                resp.getWriter().write(JSONUtil.toJson(Result.error("用户名或密码错误")));
            }
        }
    }
}