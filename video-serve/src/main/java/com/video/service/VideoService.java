package com.video.service;

import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Video;


public interface VideoService {

    Video getVideoById(Long id);

    void postVideo(Video video);
    void deleteVideo(Long videoId);

    void changeLikeVideo(Long videoId);

    void flushViewCountToDb();

    PageResult getHotTop50();

    PageResult getNewestVideos(int page, int pageSize);

    //模糊查询
    PageResult getVideoTitle(String title, int page, int pageSize, String sort);

    //按分区查询
    PageResult getVideoByCategoryId(Long categoryId, int page, int pageSize, String sort);
}
