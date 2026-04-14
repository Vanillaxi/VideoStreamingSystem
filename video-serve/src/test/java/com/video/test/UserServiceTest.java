package com.video.test;

import com.video.config.BeanFactory;
import com.video.entity.User;
import com.video.service.UserService;
import com.video.utils.JWTUtil;
import com.video.utils.RedisUtil; // 1. 引入 Redis 工具类
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private static UserService userService;

    @BeforeAll
    public static void init() {
        // 2. 测试环境必须先手动初始化 Redis，否则 Service 里的 set 操作会报空指针
        // 在实际开发中，这里通常会读取 test/resources 下的专用测试配置
        RedisUtil.initFromConfig();

        userService = BeanFactory.getBean(UserService.class);
    }

    @Test
    public void testRegisterAndLogin() {
        String testUser = "test_user_" + System.currentTimeMillis();
        String testPwd = "password123";

        // 注册逻辑
        User user = new User();
        user.setUsername(testUser);
        user.setPassword(testPwd);
        user.setRole(1);
        boolean isReg = userService.register(user);
        assertTrue(isReg, "注册应该成功");

        // 登录逻辑
        String token = userService.login(testUser, testPwd);
        assertNotNull(token, "登录成功应返回 Token");

        // 3. 验证 Redis 中是否存在对应的 Key
        // 这里的 Key 必须和业务代码里的拼接逻辑 "login:token:" + token 一致
        String redisKey = "login:token:" + token;
        String userJson = RedisUtil.get(redisKey);

        assertNotNull(userJson, "登录成功后，Redis 中应当存有用户信息");
        assertTrue(userJson.contains(testUser), "Redis 存储的用户 JSON 应当包含用户名");

        // 4. 原有的 JWT 校验
        assertEquals(testUser, JWTUtil.getUsername(token), "Token 解析出的用户名应一致");

        RedisUtil.del(redisKey);
    }

    @Test
    public void testLoginFail() {
        assertThrows(RuntimeException.class, () -> {
            userService.login("non_existent_user", "any_pwd");
        }, "用户不存在,应抛出异常");
    }
}