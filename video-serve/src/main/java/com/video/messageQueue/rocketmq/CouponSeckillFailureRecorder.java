package com.video.messageQueue.rocketmq;

import com.video.config.DBPool;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Slf4j
public class CouponSeckillFailureRecorder {
    private CouponSeckillFailureRecorder() {
    }

    public static void record(Long orderId, Long couponId, Long userId, String reason) {
        String sql = "INSERT INTO coupon_seckill_fail "
                + "(order_id, coupon_id, user_id, reason, status, create_time) "
                + "VALUES (?, ?, ?, ?, 'INIT', NOW())";
        Connection conn = DBPool.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, orderId);
            pstmt.setLong(2, couponId);
            pstmt.setLong(3, userId);
            pstmt.setString(4, reason);
            pstmt.executeUpdate();
            log.info("优惠券秒杀失败记录已保存，orderId={}, couponId={}, userId={}, reason={}",
                    orderId, couponId, userId, reason);
        } catch (Exception e) {
            log.error("保存优惠券秒杀失败记录失败，orderId={}, couponId={}, userId={}, reason={}",
                    orderId, couponId, userId, reason, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
    }
}
