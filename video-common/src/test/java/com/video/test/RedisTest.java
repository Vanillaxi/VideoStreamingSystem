package com.video.test;

import com.video.config.RedisConfiguration;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisTest {

    @Test
    public void testRedisConnection() {
        // 1. 从工具类获取连接池
        JedisPool jedisPool = RedisConfiguration.getJedisPool();

        // 2. 从池子中借用一个连接（Jedis 实例）
        // try-with-resources 会自动归还连接给池子
        try (Jedis jedis = jedisPool.getResource()) {
            // 3. 发送 PING 命令
            String response = jedis.ping();

            System.out.println("---------------------------------");
            System.out.println("Redis 响应结果: " + response); // 正常应该输出 PONG
            System.out.println("---------------------------------");

            // 4. 尝试写入和读取
            jedis.set("test_key", "Hello Redis!");
            String value = jedis.get("test_key");
            System.out.println("测试读写数据: " + value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}