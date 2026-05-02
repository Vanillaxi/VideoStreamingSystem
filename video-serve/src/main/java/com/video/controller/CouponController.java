package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.CouponSeckillRequest;
import com.video.pojo.dto.Result;
import com.video.service.CouponService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/coupon/*")
public class CouponController extends BaseController {
    @MyAutowired
    private CouponService couponService;

    @MyMapping(value = "/list", method = "GET")
    public Result list(Integer status, Integer page, Integer pageSize) {
        return Result.success(couponService.list(status, page, pageSize));
    }

    @MyMapping(value = "/detail", method = "GET")
    public Result detail(Long couponId) {
        return Result.success(couponService.detail(couponId));
    }

    @MyMapping(value = "/seckill/preDeduct", method = "POST")
    public Result seckillPreDeduct(CouponSeckillRequest request) {
        Long result = couponService.seckillPreDeduct(request);
        return Result.success(seckillResultMessage(result));
    }

    @MyMapping(value = "/order/status", method = "GET")
    public Result orderStatus(Long couponId) {
        return Result.success(couponService.orderStatus(couponId));
    }

    private String seckillResultMessage(Long result) {
        if (result == null) {
            return "抢券失败";
        }
        if (result == -1L) {
            return "系统繁忙，请稍后再试";
        }
        if (result == 0L) {
            return "抢券请求已受理";
        }
        if (result == 1L) {
            return "秒杀未开始";
        }
        if (result == 2L) {
            return "秒杀已结束";
        }
        if (result == 3L) {
            return "库存不足";
        }
        if (result == 4L) {
            return "不能重复抢券";
        }
        return "抢券失败";
    }
}
