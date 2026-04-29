package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.annotation.RequireRole;
import com.video.pojo.dto.Result;
import com.video.service.VideoService;
import jakarta.servlet.annotation.WebServlet;

/**
 * 管理员用来手动刷库测试
 */
@WebServlet("/admin/video/view-count/*")
public class AdminVideoController extends BaseController {
    @MyAutowired
    private VideoService videoService;

    @RequireRole(2)
    @MyMapping(value = "/flush", method = "POST")
    public Result flushViewCount() {
        videoService.flushViewCountToDb();
        return Result.success("播放量刷库成功");
    }
}
