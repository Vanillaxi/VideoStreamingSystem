package com.video.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminCouponCreateResponse {
    private Long couponId;
    private String message;
}
