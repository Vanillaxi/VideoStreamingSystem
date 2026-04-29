package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.mapper.FavoriteMapper;
import com.video.mapper.VideoMapper;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.Video;
import com.video.pojo.entity.VideoFavorite;
import com.video.service.FavoriteService;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@MyComponent
public class FavoriteServiceImpl implements FavoriteService {
    private static final String HOT_LIST_KEY = "video:list:hot";
    private static final String NEW_LIST_KEY = "video:list:new";
    private static final String CATEGORY_LIST_PREFIX = "video:list:category:";
    private static final String DETAIL_PREFIX = "video:detail:";
    private static final String FAVORITE_VIDEO_USER_PREFIX = "favorite:video:user:";

    @MyAutowired
    private FavoriteMapper favoriteMapper;

    @MyAutowired
    private VideoMapper videoMapper;

    /**
     * 收藏视频
     * @param videoId
     */
    @Override
    public void favorite(Long videoId) {
        Long userId = UserHolder.getUser().getId();
        Video video = videoMapper.getById(videoId);
        int rows = favoriteMapper.insertFavoriteWithTransaction(videoId, userId);
        if (rows > 0) {
            try {
                RedisUtil.zadd(favoriteKey(userId), System.currentTimeMillis(), videoId.toString());
                clearVideoCache(videoId, video.getCategoryId());
            } catch (Exception e) {
                log.warn("收藏 Redis 更新失败，videoId={}, userId={}", videoId, userId, e);
            }
        }
    }

    /**
     * 取消收藏
     * @param videoId
     */
    @Override
    public void cancelFavorite(Long videoId) {
        Long userId = UserHolder.getUser().getId();
        Video video = videoMapper.getById(videoId);
        int rows = favoriteMapper.deleteFavoriteWithTransaction(videoId, userId);
        if (rows > 0) {
            try {
                RedisUtil.zrem(favoriteKey(userId), videoId.toString());
                clearVideoCache(videoId, video.getCategoryId());
            } catch (Exception e) {
                log.warn("取消收藏 Redis 更新失败，videoId={}, userId={}", videoId, userId, e);
            }
        }
    }

    /**
     * 是否收藏
     * @param videoId
     * @return
     */
    @Override
    public Boolean isFavorite(Long videoId) {
        Long userId = UserHolder.getUser().getId();
        String key = favoriteKey(userId);
        if (!RedisUtil.exists(key)) {
            rebuildFavoriteRedis(userId);
        }
        return RedisUtil.zscore(key, videoId.toString()) != null;
    }

    /**
     * 查询收藏列表
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult getFavoriteList(int page, int pageSize) {
        Long userId = UserHolder.getUser().getId();
        String key = favoriteKey(userId);
        if (!RedisUtil.exists(key)) {
            rebuildFavoriteRedis(userId);
        }

        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1;
        List<String> videoIdSet = RedisUtil.zrevrange(key, start, end);
        Long total = RedisUtil.zcard(key);

        if (videoIdSet == null || videoIdSet.isEmpty()) {
            return new PageResult(total == null ? 0L : total, new ArrayList<>());
        }

        List<Long> videoIds = videoIdSet.stream().map(Long::valueOf).collect(Collectors.toList());
        List<Video> videos = videoMapper.getByIds(videoIds);
        Map<Long, Video> videoMap = new LinkedHashMap<>();
        for (Video video : videos) {
            videoMap.put(video.getId(), video);
        }

        List<Video> orderedVideos = new ArrayList<>();
        for (Long videoId : videoIds) {
            Video video = videoMap.get(videoId);
            if (video != null) {
                orderedVideos.add(video);
            }
        }

        return new PageResult(total == null ? 0L : total, orderedVideos);
    }

    private void clearVideoCache(Long videoId, Long categoryId) {
        RedisUtil.del(DETAIL_PREFIX + videoId);
        RedisUtil.del(HOT_LIST_KEY);
        RedisUtil.del(NEW_LIST_KEY);
        if (categoryId != null) {
            RedisUtil.del(CATEGORY_LIST_PREFIX + categoryId);
        }
    }

    private void rebuildFavoriteRedis(Long userId) {
        List<VideoFavorite> favorites = favoriteMapper.findByUserId(userId);
        String key = favoriteKey(userId);
        int index = 0;
        for (VideoFavorite favorite : favorites) {
            RedisUtil.zadd(key, favoriteScore(favorite, index), favorite.getVideoId().toString());
            index++;
        }
    }

    private double favoriteScore(VideoFavorite favorite, int index) {
        if (favorite.getCreateTime() != null) {
            return favorite.getCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }
        return System.currentTimeMillis() - index;
    }

    private String favoriteKey(Long userId) {
        return FAVORITE_VIDEO_USER_PREFIX + userId;
    }
}
