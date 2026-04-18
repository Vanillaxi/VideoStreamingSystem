package com.video.service;

import com.video.pojo.entity.User;

public interface UserService {
    // 注册
    void register(User user);

    // 登录
    String login(String username, String password);

    //登出
    void logout(String token);

    //修改信息
    void updateUserInfo(User user);

    //注销账号
    void deleteMe();

    //封号
    void removeUser(Long userId);

    //提权
    void promoteUser(Long userId, Integer role);

    //查看用户信息
    User getById(Long id);
}