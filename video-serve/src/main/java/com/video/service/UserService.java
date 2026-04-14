package com.video.service;
import com.video.entity.User;

public interface UserService {
    // 注册业务
    boolean register(User user);
    // 登录业务，返回 Token
    String login(String username, String password);
}