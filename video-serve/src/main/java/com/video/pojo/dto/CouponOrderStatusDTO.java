package com.video.pojo.dto;

import lombok.Data;

@Data
public class CouponOrderStatusDTO {
    private Long couponId;
    private Long userId;
    private Integer status;
    private String couponCode;
}
