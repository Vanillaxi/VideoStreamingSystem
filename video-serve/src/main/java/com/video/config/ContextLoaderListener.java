package com.video.config;

import com.video.messageQueue.kafka.KafkaProducerUtil;
import com.video.messageQueue.kafka.KafkaTestConsumer;
import com.video.messageQueue.kafka.KafkaTestProducer;
import com.video.messageQueue.kafka.VideoFeedCacheConsumer;
import com.video.messageQueue.kafka.VideoPublishNotificationConsumer;
import com.video.messageQueue.kafka.VideoPublishNotifyConsumer;
import com.video.proxy.BeanFactory;
import com.video.messageQueue.rocketmq.CouponSeckillTxConsumer;
import com.video.messageQueue.rocketmq.CouponSeckillTxProducer;
import com.video.task.VideoViewCountFlushTask;
import com.video.utils.CacheClient;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * 项目启动监听器：替代 Spring 的启动类功能
 * 定时任务启动位置
 */
@Slf4j
@WebListener
public class ContextLoaderListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info(">>> 正在启动视频系统 Web 容器 <<<");
        try {
            BeanFactory.init();
            SentinelRuleManager.init();
            VideoViewCountFlushTask.start();
            VideoPublishNotifyConsumer.start();
            VideoPublishNotificationConsumer.start();
            VideoFeedCacheConsumer.start();
            CouponSeckillTxConsumer.start();
            log.info("优惠券秒杀 MQ 模式：RocketMQ transaction");
            KafkaTestConsumer.start();
            log.info(">>> 所有模块初始化成功，系统已就绪 <<<");
        } catch (Exception e) {
            log.error("!!! 项目启动初始化失败 !!!", e);
            throw new RuntimeException("System start failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info(">>> 正在关闭 Web 容器，释放资源 <<<");

        try {
            UserHolder.removeUser();
            KafkaTestConsumer.shutdown();
            KafkaTestProducer.close();
            CouponSeckillTxConsumer.shutdown();
            CouponSeckillTxProducer.close();
            VideoFeedCacheConsumer.shutdown();
            VideoPublishNotificationConsumer.shutdown();
            VideoPublishNotifyConsumer.shutdown();
            KafkaProducerUtil.close();
            VideoViewCountFlushTask.shutdown();//定时刷库任务启动
            BeanFactory.destroy();
            DBPool.close();
            CacheClient.shutdown();
            RedisUtil.close();

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

            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
                log.info("【清理】MySQL 清理线程已关闭");
            } catch (Exception e) {
                log.warn("无法关闭 MySQL 清理线程，可能已经关闭或驱动版本不匹配", e);
            }

            log.info(">>> 所有资源已释放，系统安全关闭 <<<");
        } catch (Exception e) {
            log.error("!!! 资源释放时出现异常 !!!", e);
        }
    }
}
