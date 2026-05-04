package com.video.mapper;

import com.video.pojo.entity.Coupon;

import java.util.List;

public interface CouponMapper {
    Long create(Coupon coupon);

    List<Coupon> list(Integer status, int offset, int pageSize);

    Long count(Integer status);

    Coupon getById(Long id);

    Coupon getByIdForUpdate(Long id);

    int decreaseStock(Long id);

    int disable(Long id);
}
