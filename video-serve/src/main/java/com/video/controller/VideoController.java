package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Video;
import com.video.proxy.BeanFactory;
import com.video.pojo.dto.Result;
import com.video.service.VideoService;
import com.video.utils.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;

@Slf4j
@WebServlet("/video/*")
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
     * 根据title 模糊查询（分页）
     * @param title
     * @return
     */
    @MyMapping (value="/get/title",method="GET")
    public Result getVideoTitle(String title, int page, int pageSize){
        PageResult pageResult=videoService.getVideoTitle(title,page,pageSize);
        return Result.success(pageResult);
    }


    /**
     * 发布视频
     * @param video
     * @return
     */
    @MyMapping(value = "/post", method = "POST")
    public Result postVideo(Video video){
        video.setLikesCount(0L);
        videoService.postVideo(video);
        return Result.success("发布成功");
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