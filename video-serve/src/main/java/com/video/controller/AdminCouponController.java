package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.annotation.RequireRole;
import com.video.pojo.dto.AdminCouponCreateRequest;
import com.video.pojo.dto.Result;
import com.video.service.CouponService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/admin/coupon/*")
public class AdminCouponController extends BaseController {
    @MyAutowired
    private CouponService couponService;

    @RequireRole(2)
    @MyMapping(value = "/create", method = "POST")
    public Result create(AdminCouponCreateRequest request) {
        return Result.success(couponService.createByAdmin(request));
    }
}
