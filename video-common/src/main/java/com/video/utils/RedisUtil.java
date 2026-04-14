package com.video.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.InputStream;
import java.util.Properties;

public class RedisUtil {
    private static JedisPool jedisPool;

    // 初始化（由Serve模块调用）
    public static void init(String host, int port, String password, int database, int maxTotal, int maxIdle) {
        if (jedisPool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);

            // 使用支持 database 的构造函数 (超时时间默认 2000ms)
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, null, database);
            }
            System.out.println("RedisUtil 初始化成功 -> 库号: " + database + " 地址: " + host + ":" + port);
        }
    }

    public static void initFromConfig() {
        // 使用 ClassLoader 获取方式
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String configPath = "mapper/Redis.properties";

        try (InputStream is = loader.getResourceAsStream(configPath)) {
            if (is == null) {
                throw new RuntimeException("在类路径下未找到配置文件: " + configPath);
            }

            Properties prop = new Properties();
            prop.load(is);

            // 从配置中提取 database 属性
            int db = Integer.parseInt(prop.getProperty("redis.database", "0"));

            init(
                    prop.getProperty("redis.host"),
                    Integer.parseInt(prop.getProperty("redis.port")),
                    prop.getProperty("redis.password"),
                    db,
                    Integer.parseInt(prop.getProperty("redis.maxTotal")),
                    Integer.parseInt(prop.getProperty("redis.maxIdle"))
            );
        } catch (Exception e) {
            System.err.println("【Redis故障】加载配置文件失败，请确认 Common 层资源已同步。详情: " + e.getMessage());
        }
    }

    public static Jedis getJedis() {
        if (jedisPool == null) {
            throw new RuntimeException("RedisUtil未初始化，请检查启动监听器是否正确触发 initFromConfig()");
        }
        return jedisPool.getResource();
    }
    /**
     * Token 刷新机制
     */
    public static void expire(String key, int seconds) {
        try (Jedis jedis = getJedis()) {
            jedis.expire(key, seconds);
        }
    }

    public static void set(String key, String value, int seconds) {
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, seconds, value);
        }
    }

    public static String get(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.get(key);
        }
    }

    public static void del(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
        }
    }

    public static boolean exists(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.exists(key);
        }
    }
}