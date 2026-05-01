package com.video.service;

import com.video.pojo.dto.AdminCouponCreateRequest;
import com.video.pojo.dto.AdminCouponCreateResponse;
import com.video.pojo.dto.CouponOrderStatusDTO;
import com.video.pojo.dto.CouponSeckillRequest;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Coupon;

public interface CouponService {
    AdminCouponCreateResponse createByAdmin(AdminCouponCreateRequest request);

    PageResult<Coupon> list(Integer status, Integer page, Integer pageSize);

    Coupon detail(Long couponId);

    Long seckillPreDeduct(CouponSeckillRequest request);

    CouponOrderStatusDTO orderStatus(Long couponId);
}
