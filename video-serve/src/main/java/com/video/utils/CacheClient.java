package com.video.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.video.annotation.MyComponent;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

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

    public void set(String key, Object value, Long time, TimeUnit unit) {
        RedisUtil.set(key, JSONUtil.toJsonStr(value), (int) unit.toSeconds(time));
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        Long expireTime = System.currentTimeMillis() + unit.toMillis(time);
        redisData.setExpireTime(expireTime);
        RedisUtil.set(key, JSONUtil.toJsonStr(redisData), (int) TimeUnit.DAYS.toSeconds(1));
    }

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
            int expireTime = 120 + ThreadLocalRandom.current().nextInt(180);
            RedisUtil.set(key, "", expireTime);
            return null;
        }

        this.set(key, r, time, unit);
        return r;
    }

    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        String json = RedisUtil.get(key);

        if (StrUtil.isBlank(json)) {
            return null;
        }

        JSONObject rootObj = JSONUtil.parseObj(json);
        Long expireTime = rootObj.getLong("expireTime");
        R r = rootObj.getJSONObject("data").toBean(type);

        if (expireTime != null && expireTime > System.currentTimeMillis()) {
            return r;
        }

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

    public static void shutdown() {
        CACHE_REBUILD_EXECUTOR.shutdown();
        try {
            if (!CACHE_REBUILD_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                CACHE_REBUILD_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CACHE_REBUILD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
