package com.video.mapper;

import com.video.pojo.entity.Video;
import java.util.List;

public interface VideoMapper {


    Video getById(Long id);

    void postVideo(Video video);

    //对videos表
    void updateLikes(Long videoId,int i);

    List<Video> getVideoPageByTitle(String title, int offset,int pageSize);
    Long getVideoCountByTitle(String title);

    //对video_likes表
    void insertLikes(Long videoId,Long userId);
    void deleteLikes(Long videoId, Long userId);
}
