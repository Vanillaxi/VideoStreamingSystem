package com.video.pojo.dto;

import lombok.Data;

@Data
public class CouponSeckillEvent {
    private Long orderId;
    private Long couponId;
    private Long userId;
    private String couponCode;
    private Long createdAt;
}
