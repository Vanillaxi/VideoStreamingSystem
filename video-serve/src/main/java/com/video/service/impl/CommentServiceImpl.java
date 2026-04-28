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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@MyComponent
public class CommentServiceImpl implements CommentService {
    private static final String COMMENT_LIST_PREFIX = "comment:list:video:";
    private static final long COMMENT_LIST_CACHE_TTL = 60L;

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
    public void addComment(Long videoId, Long parentId, String content) {
        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        Long userId = UserHolder.getUser().getId();
        comment.setUserId(userId);
        commentMapper.insert(comment);
        clearCommentListCache(videoId);
    }

    /**
     * 根据videoId进行查询comment
     * @param videoId
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult getCommentsByVideoId(Long videoId, int page, int pageSize, String sort) {
        if (page == 1) {
            String key = buildCommentListKey(videoId, sort);
            return queryPageWithCache(key, () -> getCommentsByVideoIdFromDb(videoId, page, pageSize, sort));
        }
        return getCommentsByVideoIdFromDb(videoId, page, pageSize, sort);
    }

    private PageResult getCommentsByVideoIdFromDb(Long videoId, int page, int pageSize, String sort) {
        Long total = commentMapper.countByVideoId(videoId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<Comment> rootComments = commentMapper.findPageByVideoId(videoId, offset, pageSize, sort);
        List<Long> rootIds = rootComments.stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> replies = commentMapper.findRepliesByRootIds(rootIds);

        Map<Long, Comment> rootMap = new LinkedHashMap<>();
        for (Comment rootComment : rootComments) {
            normalizeDeletedComment(rootComment);
            rootComment.setReplies(new ArrayList<>());
            rootMap.put(rootComment.getId(), rootComment);
        }

        for (Comment reply : replies) {
            normalizeDeletedComment(reply);
            Comment rootComment = rootMap.get(reply.getRootId());
            if (rootComment != null) {
                rootComment.getReplies().add(reply);
            }
        }

        return new PageResult(total, rootComments);
    }

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    @Override
    public void delete(Long commentId) {
        Long userId = UserHolder.getUser().getId();
        Comment comment = commentMapper.findByCommentId(commentId);
        Long commentCreaterId = comment.getUserId();
        if(!userId.equals(commentCreaterId)){
            throw new DelectionNotAllowException();
        }
        commentMapper.delete(commentId);
        clearCommentListCache(comment.getVideoId());
    }


    /**
     * 更改点赞
     * @param commentId
     * @return
     */
    @Override
    public void updateLikesComment(Long commentId) {
        Long userId = UserHolder.getUser().getId();
        Comment comment = commentMapper.findByCommentId(commentId);
        String key = "comment:liked:list:" + commentId;

        //  判断是否已点赞
        Double score = RedisUtil.zscore(key, userId.toString());

        if (score == null) {
            RedisUtil.zadd(key, System.currentTimeMillis(), userId.toString());
            commentMapper.updateLikesCount(commentId, 1);
        } else {
            RedisUtil.zrem(key, userId.toString());
            commentMapper.updateLikesCount(commentId, -1);
        }
        clearCommentListCache(comment.getVideoId());
    }

    private void normalizeDeletedComment(Comment comment) {
        if (comment.getDeleted() != null && comment.getDeleted() == 1) {
            comment.setContent("该评论已删除");
        }
    }

    private PageResult queryPageWithCache(String key, java.util.function.Supplier<PageResult> dbFallback) {
        PageResult pageResult = cacheClient.queryWithLogicalExpire(key, "", PageResult.class,
                ignored -> dbFallback.get(), COMMENT_LIST_CACHE_TTL, TimeUnit.SECONDS);
        if (pageResult != null) {
            return pageResult;
        }
        PageResult dbResult = dbFallback.get();
        cacheClient.setWithLogicalExpire(key, dbResult, COMMENT_LIST_CACHE_TTL, TimeUnit.SECONDS);
        return dbResult;
    }

    private String buildCommentListKey(Long videoId, String sort) {
        return COMMENT_LIST_PREFIX + videoId + ":page1:" + normalizeSort(sort);
    }

    private String normalizeSort(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return "hot";
        }
        return "time";
    }

    private void clearCommentListCache(Long videoId) {
        RedisUtil.del(COMMENT_LIST_PREFIX + videoId + ":page1:time");
        RedisUtil.del(COMMENT_LIST_PREFIX + videoId + ":page1:hot");
    }

}
