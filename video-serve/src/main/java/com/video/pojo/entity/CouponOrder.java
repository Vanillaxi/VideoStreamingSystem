package com.video.pojo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponOrder {
    private Long id;
    private Long couponId;
    private Long userId;
    private String couponCode;
    private Integer status;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
