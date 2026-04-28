package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.AccountExitException;
import com.video.exception.AccountNotFoundException;
import com.video.exception.NotLoginException;
import com.video.exception.PasswordErrorException;
import com.video.pojo.entity.User;
import com.video.mapper.UserMapper;
import com.video.service.UserService;
import com.video.utils.JWTUtil;
import com.video.utils.OssClientUtil;
import com.video.utils.PasswordUtil;
import com.video.utils.RedisUtil;
import com.alibaba.fastjson.JSON;
import com.video.utils.UserHolder;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@MyComponent
public class UserServiceImpl implements UserService {

    @MyAutowired
    private UserMapper userMapper;

    //注册
    @Override
    public void register(User user) {
        if (userMapper.getByUsername(user.getUsername()) != null) {
            throw new AccountExitException();
        }
        String securePwd = PasswordUtil.hashPassword(user.getPassword());
        user.setPassword(securePwd);
        user.setRole(0);
        user.setCreatUser(user.getUsername());
        user.setUpdateUser(user.getUsername());
        userMapper.insert(user);
    }

    //登录
    @Override
    public String login(String username, String password) {
        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException();
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new PasswordErrorException();
        }

        //  登录成功，生成 Token
        log.info("用户 {} 登录成功，准备下发令牌", username);
        String token = JWTUtil.generate(user.getId(), user.getUsername(), user.getRole());

        // 存入 Redis
        user.setPassword(null);
        String userJson = JSON.toJSONString(user);
        String redisKey = "login:token:" + token;
        RedisUtil.set(redisKey, userJson, 3600);

        return token;
    }

    //登出
    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            throw new NotLoginException();
        }

        // 删除 Redis 中的 token 记录
        RedisUtil.del("login:token:" + token);
        UserHolder.removeUser();
        log.info("用户已登出，Token 已失效: {}", token);
    }

    /**
     * 修改信息
     * @param user
     * @return
     */
    @Override
    public void updateUserInfo(User user) {
        User currentUser = UserHolder.getUser();
        User userDto = userMapper.getByUserId(currentUser.getId());
        if (userDto == null) {
            throw new AccountNotFoundException();
        }
        
        if (user.getNickname()!=null) {userDto.setNickname(user.getNickname());}
        if (user.getPassword()!=null) {userDto.setPassword(PasswordUtil.hashPassword(user.getPassword()));}
        userDto.setUpdateUser(userDto.getUsername());
        userMapper.update(userDto);
    }

    /**
     * 换头像
     * @param file
     * @throws IOException
     */
    @Override
    public void updateAvatar(Part file) throws IOException {
        User currentUser = UserHolder.getUser();
        User user = userMapper.getByUserId(currentUser.getId());
        if (user == null) {
            throw new AccountNotFoundException();
        }

        String oldObjectKey = user.getAvatarObjectKey();
        OssClientUtil ossClientUtil = new OssClientUtil();
        OssClientUtil.UploadedObject uploadedObject = ossClientUtil.uploadAvatar(file);
        user.setAvatarUrl(uploadedObject.getUrl());
        user.setAvatarObjectKey(uploadedObject.getObjectKey());
        user.setUpdateUser(user.getUsername());
        userMapper.update(user);

        if (oldObjectKey != null && !oldObjectKey.isBlank()) {
            try {
                ossClientUtil.deleteObject(oldObjectKey);
            } catch (IOException e) {
                log.warn("旧头像 OSS 文件删除失败: {}", oldObjectKey, e);
            }
        }
    }

    /**
     * 注销自己
     */
    @Override
    public void deleteMe() {
        Long userId=UserHolder.getUser().getId();
        userMapper.delete(userId);
        String key = "user:cache:" + userId;
        RedisUtil.del(key);
        log.info("用户 {} 账号注销成功，已清理缓存", userId);
    }

    /**
     * 封号,2级以上才可执行
     * @param userId
     */
    @Override
    public void removeUser(Long userId) {
        if(userMapper.getByUserId(userId) == null) {
            throw new AccountNotFoundException();
        }
        userMapper.delete(userId);
        String key = "user:cache:" + userId;
        RedisUtil.del(key);
        log.info("用户 {} 账号注销成功，已清理缓存", userId);
    }

    /**
     * 提权，2级以上才可执行
     * @param userId
     * @param role
     * @return
     */
    @Override
    public void promoteUser(Long userId, Integer role) {
        User user=userMapper.getByUserId(userId);
        if(user==null){
            throw new AccountNotFoundException();
        }
        Long adminId=UserHolder.getUser().getId();
        user.setUpdateUser(userMapper.getByUserId(adminId).getUsername());
        user.setRole(role);
        userMapper.update(user);
    }

    /**
     * 查看用户信息
     * @param id
     * @return
     */
    @Override
    public User getById(Long id) {
        User user= userMapper.getByUserId(id);
        if(user==null){
            throw new AccountNotFoundException();
        }
        user.setPassword(null);
        return user;
    }

}
