package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.annotation.RequireRole;
import com.video.pojo.dto.Result;
import com.video.service.UserService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/admin/*")
public class AdminUserController extends BaseController {
    @MyAutowired
    private UserService userService;

    @RequireRole(2)
    @MyMapping(value = "/users", method = "GET")
    public Result listUsers(Integer page, Integer pageSize) {
        return Result.success(userService.listUsersByAdmin(page, pageSize));
    }

    @RequireRole(2)
    @MyMapping(value = "/users/search", method = "GET")
    public Result searchUsers(String nickname, String username, Integer page, Integer pageSize) {
        return Result.success(userService.searchUsersByAdmin(nickname, username, page, pageSize));
    }
}
