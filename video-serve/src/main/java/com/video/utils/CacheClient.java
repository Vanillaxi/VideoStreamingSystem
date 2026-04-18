package com.video.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.video.annotation.MyComponent;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@MyComponent
public class CacheClient {

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    private static final String LOCK_PREFIX = "lock:";

    //设置缓存
    public void set(String key, Object value, Long time, TimeUnit unit) {
        RedisUtil.set(key, JSONUtil.toJsonStr(value), (int) unit.toSeconds(time));
    }

    /**
     * 缓存重建/数据预热
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        Long expireTime = System.currentTimeMillis() + unit.toMillis(time);
        redisData.setExpireTime(expireTime);
        // 写入Redis
        RedisUtil.set(key, JSONUtil.toJsonStr(redisData), (int) TimeUnit.DAYS.toSeconds(1));
    }

    /**
     * 解决缓存穿透
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = RedisUtil.get(key);

        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        if (json != null) {
            return null;
        }

        R r = dbFallback.apply(id);

        if (r == null) {
            // 将空值写入Redis
            int expireTime = 120 + ThreadLocalRandom.current().nextInt(180);
            RedisUtil.set(key, "", expireTime);
            return null;
        }

        // 存在，存入 Redis
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 逻辑过期，需要进行数据预热
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        //System.err.println("DEBUG: [CacheClient] 尝试从Redis获取数据，Key为: ->[" + key + "]<<-");

        String json = RedisUtil.get(key);
        //System.err.println("DEBUG: [CacheClient] Redis返回的原始字符串: ->[" + json + "]<<-");

        if (StrUtil.isBlank(json)) {
            return null;
        }

        JSONObject rootObj = JSONUtil.parseObj(json);
        Long expireTime = rootObj.getLong("expireTime");
        R r = rootObj.getJSONObject("data").toBean(type);
        //System.out.println("DEBUG: 解析出来的对象内容为: " + r);

        // 未过期，直接返回
        if (expireTime != null && expireTime > System.currentTimeMillis()) {
            return r;
        }

        // 已过期，尝试重建
        String lockKey = LOCK_PREFIX + key;
        if (tryLock(lockKey)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } finally {
                    unlock(lockKey);
                }
            });
        }
        return r;
    }



    private boolean tryLock(String key) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String result = jedis.set(key, "1", SetParams.setParams().nx().ex(10));
            return "OK".equals(result);
        }
    }

    private void unlock(String key) {
        RedisUtil.del(key);
    }
}