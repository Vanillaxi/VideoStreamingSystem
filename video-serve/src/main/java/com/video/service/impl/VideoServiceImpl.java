package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.messageQueue.KafkaProducerUtil;
import com.video.pojo.dto.CursorPageResult;
import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.VideoPublishedEvent;
import com.video.pojo.entity.Video;
import com.video.pojo.entity.User;
import com.video.mapper.VideoMapper;
import com.video.service.VideoService;
import com.video.utils.CacheClient;
import com.video.utils.LuaScriptUtil;
import com.video.utils.OssClientUtil;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@MyComponent
public class VideoServiceImpl implements VideoService {
    private static final int DEFAULT_CURSOR_PAGE_SIZE = 20;
    private static final int MAX_CURSOR_PAGE_SIZE = 50;
    private static final String HOT_LIST_KEY = "video:list:hot";
    private static final String NEW_LIST_KEY = "video:list:new";
    private static final String CATEGORY_LIST_PREFIX = "video:list:category:";
    private static final String DETAIL_PREFIX = "video:detail:";
    private static final String VIEW_COUNT_PREFIX = "video:view:count:";
    private static final String VIEW_COUNT_PATTERN = VIEW_COUNT_PREFIX + "*";
    private static final String GET_AND_DELETE_SCRIPT = LuaScriptUtil.load("lua/get_and_del.lua");
    private static final long LIST_CACHE_TTL = 60L;
    private static final long DETAIL_CACHE_TTL = 300L;

    @MyAutowired
    private VideoMapper videoMapper;

    @MyAutowired
    private CacheClient cacheClient;

    /**
     * 根据视频id查视频
     * @param id
     * @return
     */
    @Override
    public Video getVideoById(Long id){
        try {
            RedisUtil.incr(VIEW_COUNT_PREFIX + id);
        } catch (Exception e) {
            log.warn("播放量 Redis 累计失败，videoId={}", id, e);
        }
        Video video = queryVideoDetailWithCache(id);

        if(video==null){
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        return video;
    }

    /**
     * 发布视频
     * @param video
     */
    @Override
    public void postVideo(Video video) {
        video.setUserId(UserHolder.getUser().getId());
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(video.getCreateTime());
        video.setStatus(video.getStatus() == null ? "PUBLISHED" : video.getStatus());
        video.setLikesCount(video.getLikesCount() == null ? 0L : video.getLikesCount());
        video.setCommentCount(video.getCommentCount() == null ? 0L : video.getCommentCount());
        video.setFavoriteCount(video.getFavoriteCount() == null ? 0L : video.getFavoriteCount());
        video.setViewCount(video.getViewCount() == null ? 0L : video.getViewCount());
        video.setHotScore(calculateHotScore(video));
        videoMapper.postVideo(video);
        sendVideoPublishedEvent(video);
        clearListCache(video.getCategoryId());
    }

    /**
     * 删除视频
     * @param videoId
     */
    @Override
    public void deleteVideo(Long videoId) {
        Video video = videoMapper.getById(videoId);
        User currentUser = UserHolder.getUser();
        if (currentUser == null || (!video.getUserId().equals(currentUser.getId()) && currentUser.getRole() < 2)) {
            throw new BusinessException(ErrorCode.NOT_ALLOW_DELETE);
        }

        try {
            new OssClientUtil().deleteObject(video.getObjectKey());
        } catch (Exception e) {
            log.warn("OSS 文件删除失败，objectKey={}", video.getObjectKey(), e);
            throw new SystemException(ErrorCode.OSS_DELETE_FAILED, e);
        }
        videoMapper.deleteById(videoId);
        clearVideoCache(videoId, video.getCategoryId());
    }

    /**
     * 更改点赞视频 (ZSet)
     * 以后可以实现好友都点赞的，然后进行推荐
     * @param videoId
     */
    @Override
    public void changeLikeVideo(Long videoId) {
        Long userId = UserHolder.getUser().getId();
        String key = "video:liked:" + videoId;

        int delta = videoMapper.changeLikeWithTransaction(videoId, userId);
        if (delta != 0) {
            Video video = videoMapper.getById(videoId);
            try {
                if (delta > 0) {
                    RedisUtil.zadd(key, (double) System.currentTimeMillis(), userId.toString());
                } else {
                    RedisUtil.zrem(key, userId.toString());
                }
                clearVideoCache(videoId, video.getCategoryId());
            } catch (Exception e) {
                log.warn("视频点赞 Redis 更新失败，videoId={}, userId={}", videoId, userId, e);
            }
        }
    }

    /**
     * 播放量刷库
     */
    @Override
    public void flushViewCountToDb() {
        log.info("开始刷新视频播放量到 MySQL");
        List<String> keys = RedisUtil.scanKeys(VIEW_COUNT_PATTERN);
        log.info("扫描到 {} 个播放量 key", keys.size());

        int successCount = 0;
        int failCount = 0;
        for (String key : keys) {
            Long videoId = parseVideoIdFromViewKey(key);
            if (videoId == null) {
                continue;
            }

            Long increment = RedisUtil.evalLong(GET_AND_DELETE_SCRIPT, Collections.singletonList(key));
            log.info("videoId={} 本轮播放量增量={}", videoId, increment);
            if (increment == null || increment <= 0) {
                continue;
            }

            try {
                Video video = videoMapper.getById(videoId);
                int rows = videoMapper.incrementViewCount(videoId, increment);
                if (rows > 0) {
                    clearVideoCacheAfterViewFlush(videoId, video.getCategoryId());
                    successCount++;
                } else {
                    RedisUtil.incrBy(key, increment);
                    failCount++;
                    log.warn("播放量刷库未影响任何行，已补回 Redis，videoId={}, increment={}", videoId, increment);
                }
            } catch (Exception e) {
                RedisUtil.incrBy(key, increment);
                failCount++;
                log.error("播放量刷库失败，已补回 Redis，videoId={}, increment={}", videoId, increment, e);
            }
        }
        log.info("播放量刷库完成，成功 {} 个，失败 {} 个", successCount, failCount);
    }

    /**
     * 根据热度查询视频（TOP50）
     * @return
     */
    @Override
    public PageResult getHotTop50() {
        return queryPageWithCache(HOT_LIST_KEY, this::getHotTop50FromDb, LIST_CACHE_TTL);
    }

    @Override
    public CursorPageResult<Video> getHotCursorPage(Double cursorHotScore, String cursorCreateTime, Long cursorId, Integer pageSize) {
        return getFeedCursorPage("hot", cursorHotScore, cursorCreateTime, cursorId, pageSize);
    }

    @Override
    public CursorPageResult<Video> getFeedCursorPage(String sort, Double cursorHotScore, String cursorCreateTime, Long cursorId, Integer pageSize) {
        String normalizedSort = normalizeSort(sort);
        int safePageSize = normalizeCursorPageSize(pageSize);
        validateCursor(normalizedSort, cursorHotScore, cursorCreateTime, cursorId);
        LocalDateTime cursorTime = parseCursorCreateTime(cursorCreateTime);

        List<Video> videoList;
        if ("hot".equals(normalizedSort)) {
            videoList = videoMapper.getHotCursorPage(cursorHotScore, cursorTime, cursorId, safePageSize + 1);
        } else {
            videoList = videoMapper.getTimeCursorPage(cursorTime, cursorId, safePageSize + 1);
        }

        boolean hasNext = videoList.size() > safePageSize;
        if (hasNext) {
            videoList = new ArrayList<>(videoList.subList(0, safePageSize));
        }

        Video lastVideo = videoList.isEmpty() ? null : videoList.get(videoList.size() - 1);
        return new CursorPageResult<>(
                videoList,
                hasNext,
                lastVideo == null ? null : lastVideo.getHotScore(),
                lastVideo == null ? null : lastVideo.getCreateTime(),
                lastVideo == null ? null : lastVideo.getId(),
                safePageSize
        );
    }

    /**
     * 根据时间查询视频
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult getNewestVideos(int page, int pageSize) {
        if (page == 1) {
            return queryPageWithCache(NEW_LIST_KEY, () -> getNewestVideosFromDb(page, pageSize), LIST_CACHE_TTL);
        }
        return getNewestVideosFromDb(page, pageSize);
    }

    /**
     * 根据title模糊查询视频
     * @param title
     * @return
     */
    @Override
    public PageResult getVideoTitle(String title, int page, int pageSize, String sort) {
        String searchTitle = (title == null || title.isEmpty()) ? "%%" : "%" + title + "%";

        Long total = videoMapper.getVideoCountByTitle(searchTitle);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<Video> videoList = videoMapper.getVideoPageByTitle(searchTitle, offset, pageSize, sort);

        return new PageResult(total, videoList);
    }

    /**
     * 根据分类ID查询视频
     * @param categoryId
     * @param page
     * @param pageSize
     * @param sort
     * @return
     */
    @Override
    public PageResult getVideoByCategoryId(Long categoryId, int page, int pageSize, String sort) {
        if (page == 1 && isTimeSort(sort)) {
            String key = CATEGORY_LIST_PREFIX + categoryId;
            return queryPageWithCache(key, () -> getVideoByCategoryIdFromDb(categoryId, page, pageSize, sort), LIST_CACHE_TTL);
        }
        return getVideoByCategoryIdFromDb(categoryId, page, pageSize, sort);
    }

    private PageResult getVideoByCategoryIdFromDb(Long categoryId, int page, int pageSize, String sort) {
        Long total = videoMapper.getVideoCountByCategoryId(categoryId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<Video> videoList = videoMapper.getVideoPageByCategoryId(categoryId, offset, pageSize, sort);

        return new PageResult(total, videoList);
    }

    //从db查视频
    private Video getByIdFromDb(Long id) {
        return videoMapper.getById(id);
    }

    private PageResult getHotTop50FromDb() {
        List<Video> videoList = videoMapper.getHotTop50();
        return new PageResult(videoList.size(), videoList);
    }

    private PageResult getNewestVideosFromDb(int page, int pageSize) {
        Long total = videoMapper.getVideoCount();
        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }
        int offset = (page - 1) * pageSize;
        List<Video> videoList = videoMapper.getNewestPage(offset, pageSize);
        return new PageResult(total, videoList);
    }

    private int normalizeCursorPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_CURSOR_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_CURSOR_PAGE_SIZE);
    }

    private String normalizeSort(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return "hot";
        }
        return "time";
    }

    private void validateCursor(String sort, Double cursorHotScore, String cursorCreateTime, Long cursorId) {
        boolean hasTime = cursorCreateTime != null && !cursorCreateTime.isBlank();
        boolean hasAnyCursor = cursorHotScore != null
                || hasTime
                || cursorId != null;
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

    private Video queryVideoDetailWithCache(Long videoId) {
        Video video = cacheClient.queryWithLogicalExpire(DETAIL_PREFIX, videoId, Video.class,
                this::getByIdFromDb, DETAIL_CACHE_TTL, TimeUnit.SECONDS);
        if (video != null) {
            return video;
        }
        Video dbVideo = getByIdFromDb(videoId);
        cacheClient.setWithLogicalExpire(DETAIL_PREFIX + videoId, dbVideo, DETAIL_CACHE_TTL, TimeUnit.SECONDS);
        return dbVideo;
    }

    private PageResult queryPageWithCache(String key, java.util.function.Supplier<PageResult> dbFallback, long ttlSeconds) {
        PageResult pageResult = cacheClient.queryWithLogicalExpire(key, "", PageResult.class,
                ignored -> dbFallback.get(), ttlSeconds, TimeUnit.SECONDS);
        if (pageResult != null) {
            return pageResult;
        }
        PageResult dbResult = dbFallback.get();
        cacheClient.setWithLogicalExpire(key, dbResult, ttlSeconds, TimeUnit.SECONDS);
        return dbResult;
    }

    private void clearVideoCache(Long videoId, Long categoryId) {
        RedisUtil.del(DETAIL_PREFIX + videoId);
        clearListCache(categoryId);
    }

    private void clearVideoCacheAfterViewFlush(Long videoId, Long categoryId) {
        try {
            clearVideoCache(videoId, categoryId);
        } catch (Exception e) {
            log.warn("播放量刷库后删除视频缓存失败，videoId={}, categoryId={}", videoId, categoryId, e);
        }
    }

    private void clearListCache(Long categoryId) {
        RedisUtil.del(HOT_LIST_KEY);
        RedisUtil.del(NEW_LIST_KEY);
        if (categoryId != null) {
            RedisUtil.del(CATEGORY_LIST_PREFIX + categoryId);
        }
    }

    private boolean isTimeSort(String sort) {
        return sort == null || sort.isBlank() || "time".equalsIgnoreCase(sort);
    }

    private Long parseVideoIdFromViewKey(String key) {
        if (key == null || !key.startsWith(VIEW_COUNT_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(key.substring(VIEW_COUNT_PREFIX.length()));
        } catch (NumberFormatException e) {
            log.warn("播放量 key 解析失败: {}", key, e);
            return null;
        }
    }

    private Double calculateHotScore(Video video) {
        long likeCount = video.getLikesCount() == null ? 0L : video.getLikesCount();
        long commentCount = video.getCommentCount() == null ? 0L : video.getCommentCount();
        long favoriteCount = video.getFavoriteCount() == null ? 0L : video.getFavoriteCount();
        long viewCount = video.getViewCount() == null ? 0L : video.getViewCount();
        return likeCount * 3D + commentCount * 5D + favoriteCount * 4D + viewCount + 100D;
    }

    private void sendVideoPublishedEvent(Video video) {
        if (video.getId() == null) {
            log.warn("视频发布 Kafka 消息跳过：videoId 为空，title={}", video.getTitle());
            return;
        }
        VideoPublishedEvent event = new VideoPublishedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setVideoId(video.getId());
        event.setAuthorId(video.getUserId());
        event.setCreatedAt(video.getCreateTime()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
        event.setHotScore(video.getHotScore());
        event.setEventType("VIDEO_PUBLISHED");
        KafkaProducerUtil.sendVideoPublished(event);
    }


}
