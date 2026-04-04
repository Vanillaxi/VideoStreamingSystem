package com.Video.Utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private static JedisPool jedisPool;

    // 初始化（由Serve模块调用）
    public static void init(String host, int port, String password, int maxTotal, int maxIdle) {
        if (jedisPool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port);
            }
            System.out.println("RedisUtil 初始化成功: " + host + ":" + port);
        }
    }

    public static Jedis getJedis() {
        if (jedisPool == null) {
            throw new RuntimeException("RedisUtil未初始化");
        }
        return jedisPool.getResource();
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