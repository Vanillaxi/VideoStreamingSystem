package com.video.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

@WebListener
@Slf4j
public class RedisContextListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 销毁连接池
        log.info("视频系统关闭：正在销毁 Redis 连接池...");
        if (RedisConfiguration.getJedisPool() != null) {
            RedisConfiguration.getJedisPool().close();
        }
    }
}