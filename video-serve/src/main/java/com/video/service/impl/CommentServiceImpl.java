package com.video.service.impl;

import com.video.annotation.MyComponent;
import com.video.basedao.CommentDao;
import com.video.entity.Comment;
import com.video.service.CommentService;
import com.video.config.BeanFactory;
import com.video.utils.UserHolder;
import java.time.LocalDateTime;
import java.util.List;

@MyComponent
public class CommentServiceImpl implements CommentService {

    private CommentDao commentDao = BeanFactory.getBean(CommentDao.class);

    /**
     * 发布评论
     * @param videoId
     * @param content
     * @return
     */
    @Override
    public boolean postComment(Long videoId, String content) {
        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());

        Long userId = UserHolder.getUser().getId();
        comment.setUserId(userId);

        return commentDao.insert(comment) > 0;
    }

    @Override
    public List<Comment> getCommentsByVideoId(Long videoId) {
        return commentDao.findByVideoId(videoId);
    }
}