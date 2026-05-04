package com.video.service;

import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.PasswordUpdateRequest;
import com.video.pojo.dto.UserAdminVO;
import com.video.pojo.dto.UserInfoVO;
import com.video.pojo.entity.User;
import jakarta.servlet.http.Part;

import java.io.IOException;

public interface UserService {
    // 注册
    void register(User user);

    // 登录
    String login(String username, String password);

    //登出
    void logout(String token);

    //修改信息
    void updateUserInfo(User user);

    // 修改密码
    void updatePassword(PasswordUpdateRequest request);

    //上传或修改头像
    void updateAvatar(Part file) throws IOException;

    //注销账号
    void deleteMe();

    //封号
    void removeUser(Long userId);

    //提权
    void promoteUser(Long userId, Integer role);

    //查看用户信息
    UserInfoVO getById(Long id);

    PageResult<UserAdminVO> listUsersByAdmin(Integer page, Integer pageSize);

    PageResult<UserAdminVO> searchUsersByAdmin(String nickname, String username, Integer page, Integer pageSize);
}
