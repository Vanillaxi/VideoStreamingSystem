package com.video.utils;

import com.video.config.RedisConfiguration;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class RedisUtil {

    //获取连接
    public static Jedis getJedis() {
        return RedisConfiguration.getJedisPool().getResource();
    }

   //统一归还入口
    public static void close(Jedis jedis) {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (Exception e) {
                throw new RuntimeException("归还 Redis 连接失败！", e);
            }
        }
    }

    // 利用 try-with-resources 实现自动归还
    public static void set(String key, String value, int seconds) {
        // 自动调用 jedis.close()，自动归还了
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            log.error("Redis set 操作失败", e);
        }
    }

    public static String get(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.get(key);
        } catch (Exception e) {
            log.error("Redis get 操作失败", e);
            return null;
        }
    }

    public static boolean exists(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.exists(key);
        } catch (Exception e) {
            log.error("Redis exists 操作失败", e);
            return false;
        }
    }

    public static Long incr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.incr(key);
        } catch (Exception e) {
            log.error("Redis incr 操作失败", e);
            return null;
        }
    }

    public static Long incrBy(String key, long increment) {
        try (Jedis jedis = getJedis()) {
            return jedis.incrBy(key, increment);
        } catch (Exception e) {
            log.error("Redis incrBy 操作失败", e);
            return null;
        }
    }

    public static List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Jedis jedis = getJedis()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(pattern).count(100);
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
            return keys;
        } catch (Exception e) {
            log.error("Redis scan 操作失败", e);
            return Collections.emptyList();
        }
    }

    public static Long evalLong(String script, List<String> keys) {
        try (Jedis jedis = getJedis()) {
            Object result = jedis.eval(script, keys, Collections.emptyList());
            if (result == null) {
                return 0L;
            }
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
            return Long.parseLong(result.toString());
        } catch (Exception e) {
            log.error("Redis eval 操作失败", e);
            return 0L;
        }
    }

    //删除token, 用于登出/注销
    public static void del(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
        }
    }

   //刷新token
    public static void expire(String key, int seconds) {
        try (Jedis jedis = getJedis()) {
            jedis.expire(key, seconds);
        } catch (Exception e) {
            log.error("Redis 刷新有效期失败", e);
        }
    }


    //set操作
    //判断成员是否在 Set 中
    public static boolean sismember(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.sismember(key, member);
        } catch (Exception e) {
            log.error("Redis sismember 失败", e);
            return false;
        }
    }

    //加成员
    public static void sadd(String key, String member) {
        try (Jedis jedis = getJedis()) {
            jedis.sadd(key, member);
        }
    }

    //删
    public static void srem(String key, String member) {
        try (Jedis jedis = getJedis()) {
            jedis.srem(key, member);
        }
    }


    //ZSet操作
    //score
    public static void zadd(String key, double score, String member) {
        try (Jedis jedis = getJedis()) {
            jedis.zadd(key, score, member);
        } catch (Exception e) {
            log.error("Redis zadd 操作失败", e);
        }
    }

    //获取score
    public static Double zscore(String key, String member) {
        try (Jedis jedis = getJedis()) {
            return jedis.zscore(key, member);
        } catch (Exception e) {
            log.error("Redis zscore 操作失败", e);
            return null;
        }
    }

    //移除score
    public static void zrem(String key, String member) {
        try (Jedis jedis = getJedis()) {
            jedis.zrem(key, member);
        } catch (Exception e) {
            log.error("Redis zrem 操作失败", e);
        }
    }

    //获取Zset成员数
    public static Long zcard(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.zcard(key);
        } catch (Exception e) {
            log.error("Redis zcard 操作失败", e);
            return 0L;
        }
    }

    /**
     * ZSet 逆序分页查询 (从大到小)
     * @param key 键
     * @param start 开始索引 (0 开始)
     * @param end 结束索引
     * @return 成员集合
     */
    public static java.util.Set<String> zrevrange(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            return (java.util.Set<String>) jedis.zrevrange(key, start, end);
        } catch (Exception e) {
            log.error("Redis zrevrange 操作失败", e);
            return null;
        }
    }

    /**
     * ZSet 正序分页查询 (从小到大)
     */
    public static java.util.Set<String> zrange(String key, long start, long end) {
        try (Jedis jedis = getJedis()) {
            return (java.util.Set<String>) jedis.zrange(key, start, end);
        } catch (Exception e) {
            log.error("Redis zrange 操作失败", e);
            return null;
        }
    }

    // 关闭 Redis 连接池
    public static void close() {
        if (RedisConfiguration.getJedisPool() != null && !RedisConfiguration.getJedisPool().isClosed()) {
            RedisConfiguration.getJedisPool().close();
            log.info("Redis 连接池已关闭");
        }
    }

}
