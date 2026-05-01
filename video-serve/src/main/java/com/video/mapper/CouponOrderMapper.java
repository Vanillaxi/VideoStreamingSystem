package com.video.mapper;

import com.video.pojo.entity.CouponOrder;

public interface CouponOrderMapper {
    CouponOrder getByCouponIdAndUserId(Long couponId, Long userId);

    int insert(CouponOrder order);
}
