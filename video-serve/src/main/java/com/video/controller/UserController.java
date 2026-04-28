package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyHeader;
import com.video.annotation.MyMapping;
import com.video.annotation.RequireRole;
import com.video.pojo.entity.User;
import com.video.pojo.dto.Result;
import com.video.service.UserService;
import com.video.utils.AuthHeaderUtil;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Part;
import java.io.IOException;

@WebServlet("/user/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class UserController extends BaseController {
    @MyAutowired
    private UserService userService;

    /**
     * 用户注册
     * @param user
     * @throws IOException
     */
    @MyMapping(value="/register",method="POST")
    public Result register(User user)  {
        userService.register(user);
        return Result.success("注册成功");
    }


    /**登录
     * @param user
     * @return
     */
    @MyMapping(value = "/login", method = "POST")
    public Result login(User user) {
        String token = userService.login(user.getUsername(), user.getPassword());
        return Result.success(token);
    }

    /**
     * 登出
     * @param token
     * @return
     */
    @MyMapping(value = "/logout", method = "POST")
    public Result logout(@MyHeader("Authorization") String authorization) {
        userService.logout(AuthHeaderUtil.extractBearerToken(authorization));
        return Result.success("退出成功");
    }

    /**
     * 修改信息
     * @param user
     * @throws IOException
     */
    @MyMapping(value="/update",method="POST")
    public Result updateUserInfo(User user) {
        userService.updateUserInfo(user);
        return Result.success("个人资料修改成功");
    }

    /**
     * 上传或修改头像
     */
    @MyMapping(value = "/avatar", method = "POST")
    public Result updateAvatar(Part file) throws IOException {
        userService.updateAvatar(file);
        return Result.success("头像修改成功");
    }

    /**
     * 查看用户信息
     * @param id
     * @return
     */
    @MyMapping(value = "/info", method = "GET")
    public Result getUserInfo(Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 注销自己
     */
    @MyMapping(value="/delete",method="DELETE")
    public Result deleteMe()  {
        userService.deleteMe();
        return Result.success("账号注销成功");
    }

    /**
     * 封号,2级以上才可执行
     * @param id
     */
    @RequireRole(2)
    @MyMapping(value="/remove",method="DELETE")
    public Result removeUser(Long id)  {
        userService.removeUser(id);
        return Result.success("封号成功");
    }


    /**
     * 提权，2级以上才可执行
     * @param userId
     * @param role
     * @throws IOException
     */
    @RequireRole(2)
    @MyMapping(value="/promote",method="POST")
    public Result promoteUser(Long userId, Integer role) {
        userService.promoteUser(userId,role);
        return Result.success("提权成功");
    }

}
