package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.mapper.VideoMapper;
import com.video.pojo.dto.FeedPageResult;
import com.video.pojo.entity.Video;
import com.video.service.FeedService;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@MyComponent
public class FeedServiceImpl implements FeedService {
    private static final String FEED_PREFIX = "feed:user:";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    @MyAutowired
    private VideoMapper videoMapper;

    @Override
    public FeedPageResult<Video> getFollowingFeed(Double lastScore, Long lastId, Integer pageSize) {
        Long userId = UserHolder.getUser().getId();
        int safePageSize = normalizePageSize(pageSize);
        List<Tuple> tuples = queryFeedTuples(userId, lastScore, lastId, safePageSize);
        boolean hasMore = tuples.size() > safePageSize;
        if (hasMore) {
            tuples = new ArrayList<>(tuples.subList(0, safePageSize));
        }

        List<Long> videoIds = tuples.stream()
                .map(tuple -> Long.valueOf(tuple.getElement()))
                .collect(Collectors.toList());
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

        Tuple lastTuple = tuples.isEmpty() ? null : tuples.get(tuples.size() - 1);
        return new FeedPageResult<>(
                orderedVideos,
                lastTuple == null ? null : lastTuple.getScore(),
                lastTuple == null ? null : Long.valueOf(lastTuple.getElement()),
                hasMore,
                safePageSize
        );
    }

    private List<Tuple> queryFeedTuples(Long userId, Double lastScore, Long lastId, int pageSize) {
        String key = FEED_PREFIX + userId;
        double max = lastScore == null ? Double.POSITIVE_INFINITY : lastScore;
        List<Tuple> rawTuples = RedisUtil.zrevrangeByScoreWithScores(key, max, 0D, 0, pageSize + 20);
        List<Tuple> filtered = new ArrayList<>();
        for (Tuple tuple : rawTuples) {
            Long videoId = Long.valueOf(tuple.getElement());
            if (lastScore != null && tuple.getScore() == lastScore && lastId != null && videoId >= lastId) {
                continue;
            }
            filtered.add(tuple);
            if (filtered.size() > pageSize) {
                break;
            }
        }
        return filtered;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
