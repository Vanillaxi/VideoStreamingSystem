package com.video.config;

import com.video.utils.RedisUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class RedisContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 程序启动时，自动读取配置文件并初始化
        RedisUtil.initFromConfig();
    }
}