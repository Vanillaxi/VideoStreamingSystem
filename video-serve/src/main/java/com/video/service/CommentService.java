package com.video.service;

import com.video.entity.Comment;
import java.util.List;

public interface CommentService {
    // 发表评论（当前用户ID和创建时间）
    boolean postComment(Long videoId, String content);

    // 获取视频下的评论列表
    List<Comment> getCommentsByVideoId(Long videoId);

}
