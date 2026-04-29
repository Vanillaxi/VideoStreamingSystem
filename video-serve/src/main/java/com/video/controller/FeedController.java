package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.Result;
import com.video.service.FeedService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/feed/*")
public class FeedController extends BaseController {
    @MyAutowired
    private FeedService feedService;

    @MyMapping(value = "/following", method = "GET")
    public Result getFollowingFeed(Double lastScore, Long lastId, Integer pageSize) {
        return Result.success(feedService.getFollowingFeed(lastScore, lastId, pageSize));
    }
}
