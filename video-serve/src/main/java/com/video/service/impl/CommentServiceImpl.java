package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.mapper.CommentMentionMapper;
import com.video.pojo.dto.CursorPageResult;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Comment;
import com.video.mapper.CommentMapper;
import com.video.mapper.NotificationMapper;
import com.video.mapper.UserMapper;
import com.video.mapper.VideoMapper;
import com.video.pojo.entity.CommentMention;
import com.video.pojo.entity.Notification;
import com.video.pojo.entity.User;
import com.video.pojo.entity.Video;
import com.video.service.CommentService;
import com.video.utils.CacheClient;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@MyComponent
public class CommentServiceImpl implements CommentService {
    private static final int DEFAULT_CURSOR_PAGE_SIZE = 20;
    private static final int MAX_CURSOR_PAGE_SIZE = 50;
    private static final String COMMENT_LIST_PREFIX = "comment:list:video:";
    private static final long COMMENT_LIST_CACHE_TTL = 60L;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_-]{1,32})");
    private static final String TYPE_COMMENT_REPLY = "COMMENT_REPLY";
    private static final String TYPE_COMMENT_MENTION = "COMMENT_MENTION";
    private static final String TYPE_VIDEO_COMMENT = "VIDEO_COMMENT";

    @MyAutowired
    private CacheClient cacheClient;

    @MyAutowired
    private CommentMapper commentMapper;

    @MyAutowired
    private CommentMentionMapper commentMentionMapper;

    @MyAutowired
    private NotificationMapper notificationMapper;

    @MyAutowired
    private UserMapper userMapper;

    @MyAutowired
    private VideoMapper videoMapper;


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
        createCommentNotifications(comment);
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

    /**
     * 游标分页查询
     * @param videoId
     * @param sort
     * @param cursorHotScore
     * @param cursorCreateTime
     * @param cursorId
     * @param pageSize
     * @return
     */
    @Override
    public CursorPageResult<Comment> getCommentsByVideoIdCursor(Long videoId, String sort, Double cursorHotScore,
                                                                String cursorCreateTime, Long cursorId, Integer pageSize) {
        String normalizedSort = normalizeSort(sort);
        int safePageSize = normalizeCursorPageSize(pageSize);
        validateCursor(normalizedSort, cursorHotScore, cursorCreateTime, cursorId);
        LocalDateTime cursorTime = parseCursorCreateTime(cursorCreateTime);

        List<Comment> rootComments;
        if ("hot".equals(normalizedSort)) {
            rootComments = commentMapper.findCursorPageByVideoIdHot(videoId, cursorHotScore, cursorTime, cursorId, safePageSize + 1);
        } else {
            rootComments = commentMapper.findCursorPageByVideoIdTime(videoId, cursorTime, cursorId, safePageSize + 1);
        }

        boolean hasNext = rootComments.size() > safePageSize;
        if (hasNext) {
            rootComments = new ArrayList<>(rootComments.subList(0, safePageSize));
        }
        assembleReplies(rootComments);

        Comment lastComment = rootComments.isEmpty() ? null : rootComments.get(rootComments.size() - 1);
        return new CursorPageResult<>(
                rootComments,
                hasNext,
                lastComment == null ? null : lastComment.getHotScore(),
                lastComment == null ? null : lastComment.getCreateTime(),
                lastComment == null ? null : lastComment.getId(),
                safePageSize
        );
    }

    private PageResult getCommentsByVideoIdFromDb(Long videoId, int page, int pageSize, String sort) {
        Long total = commentMapper.countByVideoId(videoId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<Comment> rootComments = commentMapper.findPageByVideoId(videoId, offset, pageSize, sort);
        assembleReplies(rootComments);
        return new PageResult(total, rootComments);
    }

    private void assembleReplies(List<Comment> rootComments) {
        if (rootComments == null || rootComments.isEmpty()) {
            return;
        }
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
            throw new BusinessException(ErrorCode.NOT_ALLOW_DELETE);
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

    private int normalizeCursorPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_CURSOR_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_CURSOR_PAGE_SIZE);
    }

    private void validateCursor(String sort, Double cursorHotScore, String cursorCreateTime, Long cursorId) {
        boolean hasTime = cursorCreateTime != null && !cursorCreateTime.isBlank();
        boolean hasAnyCursor = cursorHotScore != null || hasTime || cursorId != null;
        boolean hasAllTimeCursor = hasTime && cursorId != null;
        boolean hasAllHotCursor = cursorHotScore != null && hasTime && cursorId != null;
        if (!hasAnyCursor) {
            return;
        }
        if ("hot".equals(sort) && !hasAllHotCursor) {
            throw new BusinessException(400, "热度游标参数不完整");
        }
        if ("time".equals(sort) && !hasAllTimeCursor) {
            throw new BusinessException(400, "时间游标参数不完整");
        }
    }

    private LocalDateTime parseCursorCreateTime(String cursorCreateTime) {
        if (cursorCreateTime == null || cursorCreateTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursorCreateTime);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(cursorCreateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                throw new BusinessException(400, "游标时间格式错误");
            }
        }
    }

    private void createCommentNotifications(Comment comment) {
        try {
            User fromUser = UserHolder.getUser();
            LocalDateTime now = LocalDateTime.now();

            if (comment.getReplyToUserId() != null) {
                notifyIfNeeded(comment.getReplyToUserId(), fromUser.getId(), TYPE_COMMENT_REPLY,
                        displayName(fromUser) + " 回复了你的评论", comment.getId(), now);
            }

            createMentionNotifications(comment, fromUser, now);
            createVideoCommentNotification(comment, fromUser, now);
        } catch (Exception e) {
            log.warn("评论通知创建失败，commentId={}", comment.getId(), e);
        }
    }

    private void createMentionNotifications(Comment comment, User fromUser, LocalDateTime now) {
        Set<String> usernames = parseMentionUsernames(comment.getContent());
        if (usernames.isEmpty()) {
            return;
        }

        List<User> mentionedUsers = userMapper.getByUsernames(new ArrayList<>(usernames));
        for (User mentionedUser : mentionedUsers) {
            CommentMention mention = new CommentMention();
            mention.setCommentId(comment.getId());
            mention.setFromUserId(fromUser.getId());
            mention.setToUserId(mentionedUser.getId());
            mention.setIsRead(0);
            mention.setCreateTime(now);
            commentMentionMapper.insert(mention);

            notifyIfNeeded(mentionedUser.getId(), fromUser.getId(), TYPE_COMMENT_MENTION,
                    displayName(fromUser) + " 在评论中提到了你", comment.getId(), now);
        }
    }

    private void createVideoCommentNotification(Comment comment, User fromUser, LocalDateTime now) {
        Video video = videoMapper.getById(comment.getVideoId());
        notifyIfNeeded(video.getUserId(), fromUser.getId(), TYPE_VIDEO_COMMENT,
                displayName(fromUser) + " 评论了你的视频《" + video.getTitle() + "》", comment.getId(), now);
    }

    private void notifyIfNeeded(Long toUserId, Long fromUserId, String type, String content, Long relatedId, LocalDateTime now) {
        if (toUserId == null || toUserId.equals(fromUserId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(now);
        notificationMapper.insert(notification);
    }

    private Set<String> parseMentionUsernames(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return usernames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

}
