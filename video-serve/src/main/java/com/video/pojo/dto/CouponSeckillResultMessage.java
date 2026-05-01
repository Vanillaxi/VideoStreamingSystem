package com.video.pojo.dto;

import lombok.Data;

@Data
public class CouponSeckillResultMessage {
    private String type;
    private Long couponId;
    private String status;
    private String couponCode;
    private String message;
}
