package com.video.service;

import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.Result;
import com.video.pojo.entity.Comment;
import com.video.pojo.entity.Video;

import java.util.List;

public interface CommentService {
    // 发表评论
    void addComment(Long videoId, String content);

    // 获取视频下的评论列表
    PageResult getCommentsByVideoId(Long videoId,int page, int pageSize);

    //删除评论
    void delete(Long commentId);

    void updateLikesComment(Long commentId);
}
