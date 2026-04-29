package com.video.service;

import com.video.pojo.dto.FeedPageResult;
import com.video.pojo.entity.Video;

public interface FeedService {
    FeedPageResult<Video> getFollowingFeed(Double lastScore, Long lastId, Integer pageSize);
}
