package com.video.test;

import com.video.proxy.BeanFactory;
import com.video.service.UserService;
import org.junit.Before; // 注意：JUnit4 使用 @Before
import org.junit.Test;

public class IocTest {

    @Before
    public void init() {
        // 引用 BeanFactory ，触发它的 static 静态代码块执行扫描
        try {
            Class.forName("com.video.proxy.BeanFactory");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetBean() {
        UserService userService = BeanFactory.getBean(UserService.class);

        if (userService != null) {
            System.out.println("---------------------------------");
            System.out.println("成功拿到 UserService 实例！");
            System.out.println("实例对象：" + userService);
            System.out.println("---------------------------------");
        }
    }
}