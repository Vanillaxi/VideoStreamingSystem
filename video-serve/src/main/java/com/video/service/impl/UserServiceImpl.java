package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.AuthException;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.pojo.entity.User;
import com.video.mapper.UserMapper;
import com.video.mapper.FollowMapper;
import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.PasswordUpdateRequest;
import com.video.pojo.dto.UserAdminVO;
import com.video.pojo.dto.UserInfoVO;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@MyComponent
public class UserServiceImpl implements UserService {

    @MyAutowired
    private UserMapper userMapper;

    @MyAutowired
    private FollowMapper followMapper;

    //注册
    @Override
    public void register(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }
        if (userMapper.getByUsername(user.getUsername()) != null) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXIST);
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
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_ERROR);
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
            throw new AuthException(ErrorCode.USER_NOT_LOGIN);
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
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        
        if (user.getNickname()!=null) {userDto.setNickname(user.getNickname());}
        userDto.setUpdateUser(userDto.getUsername());
        userMapper.updateProfile(userDto);
    }

    @Override
    public void updatePassword(PasswordUpdateRequest request) {
        if (request == null || isBlank(request.getOldPassword()) || isBlank(request.getNewPassword())) {
            throw new BusinessException(400, "旧密码和新密码不能为空");
        }
        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new AuthException(ErrorCode.USER_NOT_LOGIN);
        }
        User dbUser = userMapper.getByUserId(currentUser.getId());
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!PasswordUtil.checkPassword(request.getOldPassword(), dbUser.getPassword())) {
            throw new BusinessException(401, "旧密码错误");
        }
        userMapper.updatePassword(dbUser.getId(), PasswordUtil.hashPassword(request.getNewPassword()), dbUser.getUsername());
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
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        String oldObjectKey = user.getAvatarObjectKey();
        OssClientUtil ossClientUtil = new OssClientUtil();
        OssClientUtil.UploadedObject uploadedObject = ossClientUtil.uploadAvatar(file);
        user.setAvatarUrl(uploadedObject.getUrl());
        user.setAvatarObjectKey(uploadedObject.getObjectKey());
        user.setUpdateUser(user.getUsername());
        userMapper.updateAvatar(user);

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
        User targetUser = userMapper.getByUserId(userId);
        if(targetUser == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        User adminUser = getCurrentAdmin();
        checkCanManageLowerRole(adminUser, targetUser);
        userMapper.delete(userId);
        String key = "user:cache:" + userId;
        RedisUtil.del(key);
        log.info("管理员 {} 注销用户 {} 成功，已清理缓存", adminUser.getId(), userId);
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
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        User adminUser = getCurrentAdmin();
        checkCanManageLowerRole(adminUser, user);
        checkTargetRoleLowerThanAdmin(adminUser, role);
        user.setUpdateUser(adminUser.getUsername());
        user.setRole(role);
        userMapper.updateRole(user);
        log.info("管理员 {} 将用户 {} 权限调整为 {}", adminUser.getId(), userId, role);
    }

    private User getCurrentAdmin() {
        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new AuthException(ErrorCode.USER_NOT_LOGIN);
        }
        User adminUser = userMapper.getByUserId(currentUser.getId());
        if (adminUser == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return adminUser;
    }

    private void checkCanManageLowerRole(User adminUser, User targetUser) {
        if (targetUser.getId() != null && targetUser.getId().equals(adminUser.getId())) {
            throw new BusinessException(403, "不能操作自己的账号");
        }
        int adminRole = safeRole(adminUser.getRole());
        int targetRole = safeRole(targetUser.getRole());
        if (targetRole >= adminRole) {
            throw new BusinessException(403, "只能操作比自己等级低的用户");
        }
    }

    private void checkTargetRoleLowerThanAdmin(User adminUser, Integer targetRole) {
        if (targetRole == null || targetRole < 0) {
            throw new BusinessException(400, "目标权限等级不合法");
        }
        int adminRole = safeRole(adminUser.getRole());
        if (adminRole < 3 && targetRole >= adminRole) {
            throw new BusinessException(403, "只能将用户提权到低于自己的等级");
        }
        if (adminRole >= 3 && targetRole > 3) {
            throw new BusinessException(403, "目标权限等级不合法");
        }
    }

    private int safeRole(Integer role) {
        return role == null ? 0 : role;
    }

    /**
     * 查看用户信息
     * @param id
     * @return
     */
    @Override
    public UserInfoVO getById(Long id) {
        User user= userMapper.getByUserId(id);
        if(user==null){
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return toUserInfoVO(user);
    }

    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setTargetUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setAvatar(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setFollowCount(safeCount(followMapper.countFollowings(user.getId())));
        vo.setFanCount(safeCount(followMapper.countFollowers(user.getId())));
        vo.setMutualFollowCount(safeCount(followMapper.countFriends(user.getId())));
        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().equals(user.getId())) {
            vo.setIsFollowed(false);
        } else {
            vo.setIsFollowed(Boolean.TRUE.equals(followMapper.isFollow(user.getId(), currentUser.getId())));
        }
        return vo;
    }

    @Override
    public PageResult<UserAdminVO> listUsersByAdmin(Integer page, Integer pageSize) {
        checkAdminAccess();
        int currentPage = normalizePage(page);
        int currentPageSize = normalizePageSize(pageSize);
        int offset = (currentPage - 1) * currentPageSize;
        Long total = userMapper.countUsers();
        List<User> users = userMapper.listUsers(offset, currentPageSize);
        return new PageResult<>(total == null ? 0L : total, toAdminVOList(users));
    }

    @Override
    public PageResult<UserAdminVO> searchUsersByAdmin(String nickname, String username, Integer page, Integer pageSize) {
        checkAdminAccess();
        int currentPage = normalizePage(page);
        int currentPageSize = normalizePageSize(pageSize);
        int offset = (currentPage - 1) * currentPageSize;
        String nicknameLike = isBlank(nickname) ? null : "%" + nickname.trim() + "%";
        String usernameLike = isBlank(username) ? null : "%" + username.trim() + "%";
        Long total = userMapper.countSearchUsers(nicknameLike, usernameLike);
        List<User> users = userMapper.searchUsers(nicknameLike, usernameLike, offset, currentPageSize);
        return new PageResult<>(total == null ? 0L : total, toAdminVOList(users));
    }

    private void checkAdminAccess() {
        User adminUser = getCurrentAdmin();
        if (safeRole(adminUser.getRole()) < 2) {
            throw new BusinessException(403, "权限不足，请联系香草管理员");
        }
    }

    private List<UserAdminVO> toAdminVOList(List<User> users) {
        List<UserAdminVO> result = new ArrayList<>();
        if (users == null) {
            return result;
        }
        for (User user : users) {
            UserAdminVO vo = new UserAdminVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setRole(user.getRole());
            vo.setStatus("正常");
            vo.setCreateTime(user.getCreateTime());
            vo.setUpdateTime(user.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }

}
