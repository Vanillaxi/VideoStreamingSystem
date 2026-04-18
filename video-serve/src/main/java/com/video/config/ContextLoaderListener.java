package com.video.config;

import com.video.proxy.BeanFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 项目启动监听器：替代 Spring 的启动类功能
 */
@Slf4j
@WebListener //  Tomcat 启动时自动运行这个类
public class ContextLoaderListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info(">>> 正在启动视频系统 Web 容器 <<<");
        try {
            // 1. 初始化 BeanFactory (扫描注解、创建单例、依赖注入)
            BeanFactory.init();

            // 2. 初始化 XML SQL 读取器 (如果有静态块也可以在里面触发)
            log.info(">>> 所有模块初始化成功，系统已就绪 <<<");
        } catch (Exception e) {
            log.error("!!! 项目启动初始化失败 !!!", e);
            // 这里建议抛出运行时异常，让 Tomcat 启动失败，而不是带病运行
            throw new RuntimeException("System start failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info(">>> 正在关闭 Web 容器，释放资源 <<<");
        // 这里可以写关闭数据库连接池、关闭 Redis 连接的代码
    }
}