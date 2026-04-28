package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.Result;
import com.video.service.FavoriteService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/favorite/*")
public class FavoriteController extends BaseController {
    @MyAutowired
    private FavoriteService favoriteService;

    @MyMapping(value = "/add", method = "POST")
    public Result favorite(Long videoId) {
        favoriteService.favorite(videoId);
        return Result.success("收藏成功");
    }

    @MyMapping(value = "/cancel", method = "DELETE")
    public Result cancelFavorite(Long videoId) {
        favoriteService.cancelFavorite(videoId);
        return Result.success("取消收藏成功");
    }

    @MyMapping(value = "/check", method = "GET")
    public Result isFavorite(Long videoId) {
        return Result.success(favoriteService.isFavorite(videoId));
    }

    @MyMapping(value = "/list", method = "GET")
    public Result getFavoriteList(int page, int pageSize) {
        return Result.success(favoriteService.getFavoriteList(page, pageSize));
    }
}
