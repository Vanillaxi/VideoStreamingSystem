package com.video.config;

import com.video.proxy.BeanFactory;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

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
            //  初始化 BeanFactory (扫描注解、创建单例、依赖注入)，初始化 XML SQL 读取器
            BeanFactory.init();
            log.info(">>> 所有模块初始化成功，系统已就绪 <<<");
        } catch (Exception e) {
            //抛出异常，启动失败
            log.error("!!! 项目启动初始化失败 !!!", e);
            throw new RuntimeException("System start failed", e);
        }
    }

//    @Override
//    public void contextDestroyed(ServletContextEvent sce) {
//        log.info(">>> 正在关闭 Web 容器，释放资源 <<<");
//
//        try {
//            UserHolder.removeUser();
//            BeanFactory.destroy();
//            DBPool.close();
//            RedisUtil.close();
//            log.info(">>> 所有资源已释放，系统安全关闭 <<<");
//        } catch (Exception e) {
//            log.error("!!! 资源释放时出现异常 !!!", e);
//        }
//    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info(">>> 正在关闭 Web 容器，释放资源 <<<");

        try {

            UserHolder.removeUser();
            BeanFactory.destroy();
            DBPool.close();
            RedisUtil.close();

            // 手动注销 JDBC 驱动
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(driver);
                    log.info("【清理】注销 JDBC 驱动: {}", driver);
                } catch (Exception e) {
                    log.error("注销驱动失败: {}", driver, e);
                }
            }

            // 停止 MySQL 遗留的清理线程 (解决 IllegalStateException 报错)
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
                log.info("【清理】MySQL 清理线程已强制关闭");
            } catch (Exception e) {
                log.warn("无法关闭 MySQL 清理线程，可能驱动版本不一致或已关闭");
            }

            log.info(">>> 所有资源已释放，系统安全关闭 <<<");
        } catch (Exception e) {
            log.error("!!! 资源释放时出现异常 !!!", e);
        }
    }


}