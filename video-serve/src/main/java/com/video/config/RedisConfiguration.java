package com.video.config;

import com.video.utils.AppProperties;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Properties;

@Slf4j
public class RedisConfiguration {
    private static JedisPool jedisPool;

    static {
        try {
            Properties props = AppProperties.getProperties();

            // 1. 配置连接池参数
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(Integer.parseInt(props.getProperty("redis.maxTotal", "20")));
            config.setMaxIdle(Integer.parseInt(props.getProperty("redis.maxIdle", "10")));

            // 2. 读取配置信息，设置了默认值
            String host = props.getProperty("redis.host", "localhost");
            int port = Integer.parseInt(props.getProperty("redis.port", "6379"));
            String password = props.getProperty("redis.password");
            int database = Integer.parseInt(props.getProperty("redis.database", "1"));
            // 设置超时时间，默认 2000ms
            int timeout = 2000;

            // 3. 创建带密码和 DB 选择的连接池
            jedisPool = new JedisPool(config, host, port, timeout, password, database);

            log.info("Redis连接池初始化成功，主机: {}, 端口: {}, 使用库: {}", host, port, database);

        } catch (Exception e) {
            log.error("Redis连接池初始化出现严重错误！", e);
        }
    }

    public static JedisPool getJedisPool() {
        return jedisPool;
    }
}
