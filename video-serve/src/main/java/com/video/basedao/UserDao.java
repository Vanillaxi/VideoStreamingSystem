package com.video.basedao;

import com.video.entity.User;

public interface UserDao {
    int save(User user);                    // 注册
    User getByUsername(String username);    // 登录

}