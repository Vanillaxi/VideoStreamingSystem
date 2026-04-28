package com.video.service;

import com.video.pojo.dto.PageResult;
public interface CommentService {
    // 发表评论
    void addComment(Long videoId, Long parentId, String content);

    // 获取视频下的评论列表
    PageResult getCommentsByVideoId(Long videoId,int page, int pageSize, String sort);

    //删除评论
    void delete(Long commentId);

    void updateLikesComment(Long commentId);
}
