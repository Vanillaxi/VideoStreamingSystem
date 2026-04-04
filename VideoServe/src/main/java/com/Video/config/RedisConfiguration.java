package com.Video.config;

import com.Video.Utils.LoggerUtil;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.io.InputStream;
import java.util.Properties;

public class RedisConfiguration {
    private static JedisPool jedisPool;

    static {
        // 使用 try-with-resources 确保流关闭
        try (InputStream is = RedisConfiguration.class.getClassLoader().getResourceAsStream("mapper/Redis.properties")) {
            Properties props = new Properties();
            if (is != null) {
                props.load(is);

                // 1. 配置连接池参数
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(Integer.parseInt(props.getProperty("redis.maxTotal", "20")));
                config.setMaxIdle(Integer.parseInt(props.getProperty("redis.maxIdle", "10")));

                // 2. 读取配置信息，这里保险起见，设置了默认值
                String host = props.getProperty("redis.host", "localhost");
                int port = Integer.parseInt(props.getProperty("redis.port", "6379"));
                String password = props.getProperty("redis.password");
                int database = Integer.parseInt(props.getProperty("redis.database", "1"));
                // 设置超时时间，默认 2000ms
                int timeout = 2000;

                // 3. 创建带密码和 DB 选择的连接池
                // 参数顺序：config, host, port, timeout, password, database
                jedisPool = new JedisPool(config, host, port, timeout, password, database);

                LoggerUtil.info(RedisConfiguration.class, "Redis连接池初始化成功，当前使用库: " + database);
            } else {
                LoggerUtil.warn(RedisConfiguration.class, "未找到 Redis.properties 配置文件");
            }
        } catch (Exception e) {
            LoggerUtil.error(RedisConfiguration.class, "Redis连接池初始化失败", e);
        }
    }

    public static JedisPool getJedisPool() {
        return jedisPool;
    }
}