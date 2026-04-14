package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.basedao.UserDao;
import com.video.entity.User;
import com.video.service.UserService;
import com.video.utils.JWTUtil;
import com.video.utils.PasswordUtil;
import com.video.utils.RedisUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MyComponent
public class UserServiceImpl implements UserService {

    @MyAutowired
    private UserDao userDao;

    @Override
    public boolean register(User user) {
        if (userDao.getByUsername(user.getUsername()) != null) {
            throw new RuntimeException("该用户名已被注册");
        }
        String securePwd = PasswordUtil.hashPassword(user.getPassword());
        user.setPassword(securePwd);
        return userDao.save(user) > 0;
    }

    @Override
    public String login(String username, String password) {
        // 1. 获取用户信息
        User user = userDao.getByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 校验密码
        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 登录成功，生成 Token
        log.info("用户 {} 登录成功，准备下发令牌", username);
        String token = JWTUtil.generate(user.getId(), user.getUsername(), user.getRole());

        // 4. 存入 Redis（必须加上和 Filter 一致的前缀）
        String userJson = JSON.toJSONString(user);
        String redisKey = "login:token:" + token;

        RedisUtil.set(redisKey, userJson, 1800);

        log.info("Token 已存入 Redis 1号库，Key 为: {}，过期时间 30min", redisKey);

        return token;
    }
}