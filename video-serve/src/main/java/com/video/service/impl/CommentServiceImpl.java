package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.DelectionNotAllowException;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Comment;
import com.video.mapper.CommentMapper;
import com.video.service.CommentService;
import com.video.utils.CacheClient;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@MyComponent
public class CommentServiceImpl implements CommentService {
    @MyAutowired
    private CacheClient cacheClient;

    @MyAutowired
    private CommentMapper commentMapper;


    /**
     * 发布评论
     * @param videoId
     * @param content
     */
    @Override
    public void addComment(Long videoId, String content) {
        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        Long userId = UserHolder.getUser().getId();
        comment.setUserId(userId);
        commentMapper.insert(comment);
    }

    /**
     * 根据videoId进行查询comment
     * @param videoId
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult getCommentsByVideoId(Long videoId, int page, int pageSize) {
        Long total = commentMapper.countByVideoId(videoId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<Comment> commentList = commentMapper.findPageByVideoId ( videoId, offset, pageSize);
        return new PageResult(total, commentList);
    }

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    @Override
    public void delete(Long commentId) {
        Long userId = UserHolder.getUser().getId();
        Long commentCreaterId = commentMapper.findByCommentId(commentId).getUserId();
        if(!userId.equals(commentCreaterId)){
            throw new DelectionNotAllowException();
        }
        commentMapper.delete(commentId);
    }


    /**
     * 更改点赞
     * @param commentId
     * @return
     */
    @Override
    public void updateLikesComment(Long commentId) {
        Long userId = UserHolder.getUser().getId();
        String key = "comment:liked:list:" + commentId;

        // 1. 判断是否已点赞
        Double score = RedisUtil.zscore(key, userId.toString());

        if (score == null) {
            RedisUtil.zadd(key, System.currentTimeMillis(), userId.toString());
            commentMapper.updateLikesCount(commentId, 1);
        } else {
            RedisUtil.zrem(key, userId.toString());
            commentMapper.updateLikesCount(commentId, -1);
        }
    }

}