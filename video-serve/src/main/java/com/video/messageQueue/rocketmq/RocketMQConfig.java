package com.video.messageQueue.rocketmq;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class RocketMQConfig {
    private static final String CONFIG_FILE = "properties/RocketMQ.properties";
    private static final Properties PROPERTIES = loadConfig();

    private RocketMQConfig() {
    }

    public static String nameSrvAddr() {
        return PROPERTIES.getProperty("rocketmq.namesrvAddr", "localhost:9876");
    }

    public static String couponSeckillTxTopic() {
        return PROPERTIES.getProperty("rocketmq.topic.couponSeckillTx", "coupon_seckill_tx");
    }

    public static String couponSeckillTxProducerGroup() {
        return PROPERTIES.getProperty("rocketmq.producerGroup.couponSeckillTx", "coupon_seckill_tx_producer_group");
    }

    public static String couponSeckillTxConsumerGroup() {
        return PROPERTIES.getProperty("rocketmq.consumerGroup.couponSeckillTx", "coupon_seckill_tx_consumer_group");
    }

    public static int couponSeckillTxMaxReconsumeTimes() {
        return Integer.parseInt(PROPERTIES.getProperty("rocketmq.consumerMaxReconsumeTimes.couponSeckillTx", "16"));
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                log.warn("未找到 RocketMQ 配置文件 {}，使用默认配置", CONFIG_FILE);
            }
        } catch (Exception e) {
            log.warn("读取 RocketMQ 配置失败，使用默认配置", e);
        }
        return properties;
    }
}
