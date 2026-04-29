package com.video.service;

import com.video.pojo.dto.CursorPageResult;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Comment;

public interface CommentService {
    // 发表评论
    void addComment(Long videoId, Long parentId, String content);

    // 获取视频下的评论列表
    PageResult getCommentsByVideoId(Long videoId,int page, int pageSize, String sort);
    CursorPageResult<Comment> getCommentsByVideoIdCursor(Long videoId, String sort, Double cursorHotScore,
                                                         String cursorCreateTime, Long cursorId, Integer pageSize);

    //删除评论
    void delete(Long commentId);

    void updateLikesComment(Long commentId);
}
