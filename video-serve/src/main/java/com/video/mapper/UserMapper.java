package com.video.mapper;

import com.video.pojo.entity.User;
import java.util.List;

public interface UserMapper {
    void insert(User user);// 注册

    User getByUsername(String username);
    User getByUserId(Long userId);
    List<User> getByIds(List<Long> userIds);
    List<User> getByUsernames(List<String> usernames);

    void update(User user); //修改信息

    void delete(Long userId);//注销账号
}
