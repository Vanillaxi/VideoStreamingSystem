package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Video;
import com.video.pojo.dto.Result;
import com.video.service.VideoService;
import com.video.utils.OssClientUtil;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
@WebServlet("/video/*")
@MultipartConfig(maxFileSize = 500 * 1024 * 1024L, maxRequestSize = 520 * 1024 * 1024L)
public class VideoController extends BaseController {
    @MyAutowired
    private VideoService videoService;

    /**
     * 根据id查询，应在模糊查询之后
     * @param id
     * @return
     */
    @MyMapping (value="/get/id",method="GET")
    public Result getVideo(Long id){
        Video video= videoService.getVideoById(id);
        return Result.success(video);
    }

    /**
     * 根据title 模糊查询
     * @param title
     * @return
     */
    @MyMapping (value="/get/title",method="GET")
    public Result getVideoTitle(String title, int page, int pageSize, String sort){
        PageResult pageResult=videoService.getVideoTitle(title,page,pageSize,sort);
        return Result.success(pageResult);
    }

    /**
     * 首页热门视频 Top50
     */
    @MyMapping(value = "/list/hot", method = "GET")
    public Result getHotTop50() {
        return Result.success(videoService.getHotTop50());
    }

    /**
     * 热度游标分页。第一页不传游标，后续传上一页返回的 nextHotScore、nextCreateTime、nextId。
     */
    @MyMapping(value = "/list/hot/cursor", method = "GET")
    public Result getHotCursorPage(Double cursorHotScore, String cursorCreateTime, Long cursorId, Integer pageSize) {
        return Result.success(videoService.getHotCursorPage(cursorHotScore, cursorCreateTime, cursorId, pageSize));
    }

    /**
     * Feed 流游标分页。sort=time/hot；第一页不传游标，后续传上一页返回的游标。
     */
    @MyMapping(value = "/feed/cursor", method = "GET")
    public Result getFeedCursorPage(String sort, Double cursorHotScore, String cursorCreateTime, Long cursorId, Integer pageSize) {
        return Result.success(videoService.getFeedCursorPage(sort, cursorHotScore, cursorCreateTime, cursorId, pageSize));
    }

    /**
     * 最新视频列表
     */
    @MyMapping(value = "/list/new", method = "GET")
    public Result getNewestVideos(int page, int pageSize) {
        return Result.success(videoService.getNewestVideos(page, pageSize));
    }

    /**
     * 根据分区查询视频
     * @param categoryId
     * @return
     */
    @MyMapping(value = "/get/category", method = "GET")
    public Result getVideoByCategory(Long categoryId, int page, int pageSize, String sort) {
        PageResult pageResult = videoService.getVideoByCategoryId(categoryId, page, pageSize, sort);
        return Result.success(pageResult);
    }


    /**
     * 发布视频
     * @param title
     * @param file
     * @return
     */
    @MyMapping(value = "/post", method = "POST")
    public Result postVideo(String title, String description, Long categoryId, Part file){
        if (title == null || title.isBlank()) {
            return Result.error("视频标题不能为空");
        }

        OssClientUtil.UploadedObject uploadedObject;
        try {
            uploadedObject = new OssClientUtil().uploadVideo(file);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            log.warn("视频上传失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }

        Video video = new Video();

        video.setTitle(title);
        video.setDescription(description);
        video.setCategoryId(categoryId);
        video.setVideoUrl(uploadedObject.getUrl());
        video.setObjectKey(uploadedObject.getObjectKey());
        video.setSize(uploadedObject.getSize());
        video.setStatus("PUBLISHED");
        video.setLikesCount(0L);
        video.setCommentCount(0L);
        video.setFavoriteCount(0L);
        video.setViewCount(0L);
        videoService.postVideo(video);
        return Result.success(video);
    }


    /**
     * 删除视频，同时删除 OSS 文件。
     * @param videoId
     * @return
     */
    @MyMapping(value = "/delete", method = "DELETE")
    public Result deleteVideo(Long videoId) {
        videoService.deleteVideo(videoId);
        return Result.success("删除成功");
    }

    /**
     * 点赞视频
     * @param videoId
     * @return
     */
    @MyMapping (value="/changeLikes",method="POST")
    public Result changeLikeVideo(Long videoId){
        videoService.changeLikeVideo(videoId);
        return Result.success("更新成功");
    }

}
