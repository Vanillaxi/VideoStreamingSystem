package com.video.service;

import com.video.entity.Video;


public interface VideoService {
    /**
     * 根据id查询
     * @param id
     * @return
     */
    Video getVideoById(Long id);
}
