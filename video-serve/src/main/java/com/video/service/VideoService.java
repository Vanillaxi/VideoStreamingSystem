package com.video.service;

import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Video;


public interface VideoService {

    Video getVideoById(Long id);

    void postVideo(Video video);

    void changeLikeVideo(Long videoId);

    //模糊查询
    PageResult getVideoTitle(String title, int page, int pageSize);
}
