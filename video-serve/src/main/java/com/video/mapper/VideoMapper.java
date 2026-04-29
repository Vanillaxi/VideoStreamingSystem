package com.video.mapper;

import com.video.pojo.entity.Video;
import java.time.LocalDateTime;
import java.util.List;

public interface VideoMapper {


    Video getById(Long id);
    List<Video> getByIds(List<Long> ids);

    void postVideo(Video video);
    void deleteById(Long videoId);

    //对videos表
    void updateLikes(Long videoId,int i);
    int changeLikeWithTransaction(Long videoId, Long userId);
    void updateCommentCount(Long videoId, int count);
    void updateFavoriteCount(Long videoId, int count);
    int incrementViewCount(Long videoId, long increment);

    List<Video> getVideoPageByTitle(String title, int offset,int pageSize, String sort);
    Long getVideoCountByTitle(String title);
    List<Video> getVideoPageByCategoryId(Long categoryId, int offset, int pageSize, String sort);
    Long getVideoCountByCategoryId(Long categoryId);
    List<Video> getHotTop50();
    List<Video> getHotCursorPage(Double cursorHotScore, LocalDateTime cursorCreateTime, Long cursorId, int limit);
    List<Video> getTimeCursorPage(LocalDateTime cursorCreateTime, Long cursorId, int limit);
    List<Video> getNewestPage(int offset, int pageSize);
    Long getVideoCount();

    //对video_likes表
    void insertLikes(Long videoId,Long userId);
    void deleteLikes(Long videoId, Long userId);
}
