package com.video.mapper;

import com.video.pojo.entity.User;
import java.util.List;

public interface UserMapper {
    void insert(User user);// 注册

    User getByUsername(String username);
    User getByUserId(Long userId);
    List<User> getByIds(List<Long> userIds);
    List<User> getByUsernames(List<String> usernames);
    List<User> listUsers(int offset, int pageSize);
    Long countUsers();
    List<User> searchUsers(String nicknameLike, String usernameLike, int offset, int pageSize);
    Long countSearchUsers(String nicknameLike, String usernameLike);

    void update(User user); //修改信息
    void updateProfile(User user);
    void updateAvatar(User user);
    void updateRole(User user);
    void updatePassword(Long userId, String password, String updateUser);

    void delete(Long userId);//注销账号
}
