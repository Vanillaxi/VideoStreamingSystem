package com.video.messageQueue.rocketmq;

import com.video.pojo.dto.CouponSeckillEvent;
import com.video.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CouponSeckillRedisCompensator {
    private static final String COUPON_STOCK_PREFIX = "coupon:seckill:stock:";
    private static final String COUPON_USERS_PREFIX = "coupon:seckill:users:";

    private CouponSeckillRedisCompensator() {
    }

    public static boolean compensate(CouponSeckillEvent event, String reason) {
        try {
            Long stock = RedisUtil.incrBy(stockKey(event.getCouponId()), 1);
            if (stock == null) {
                throw new IllegalStateException("Redis 库存回补返回空");
            }
            RedisUtil.srem(usersKey(event.getCouponId()), event.getUserId().toString());
            log.info("Redis 秒杀预扣补偿成功，orderId={}, couponId={}, userId={}, stock={}, reason={}",
                    event.getOrderId(), event.getCouponId(), event.getUserId(), stock, reason);
            return true;
        } catch (Exception e) {
            log.error("Redis 秒杀预扣补偿失败，orderId={}, couponId={}, userId={}, reason={}",
                    event.getOrderId(), event.getCouponId(), event.getUserId(), reason, e);
            CouponSeckillFailureRecorder.record(event.getOrderId(), event.getCouponId(), event.getUserId(),
                    "Redis补偿失败: " + reason);
            return false;
        }
    }

    private static String stockKey(Long couponId) {
        return COUPON_STOCK_PREFIX + couponId;
    }

    private static String usersKey(Long couponId) {
        return COUPON_USERS_PREFIX + couponId;
    }
}
