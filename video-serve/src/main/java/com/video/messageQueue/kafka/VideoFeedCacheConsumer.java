package com.video.messageQueue.kafka;

import com.video.pojo.dto.VideoPublishedEvent;
import com.video.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VideoFeedCacheConsumer {
    private static final String FEED_LATEST_KEY = "feed:latest";
    private static final String VIDEO_LIST_NEW_KEY = "video:list:new";
    private static final String VIDEO_DETAIL_PREFIX = "video:detail:";
    private static final int MAX_LATEST_SIZE = 1000;
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ExecutorService executorService;

    private VideoFeedCacheConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("视频发布 Feed 缓存 Consumer 已启动，跳过重复启动");
            return;
        }
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "video-feed-cache-consumer");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(() -> VideoPublishedEventConsumerSupport.consumeLoop(
                "视频发布 Feed 缓存 Consumer",
                STARTED,
                "kafka.video.feed-cache.group.id",
                "video-feed-cache-group",
                VideoFeedCacheConsumer::handleEvent
        ));
        log.info("视频发布 Feed 缓存 Consumer 启动成功");
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        VideoPublishedEventConsumerSupport.shutdownExecutor(executorService);
        executorService = null;
        log.info("视频发布 Feed 缓存 Consumer 已关闭");
    }

    private static void handleEvent(VideoPublishedEvent event) {
        try {
            long score = event.getEventTime() != null
                    ? event.getEventTime()
                    : event.getCreatedAt() == null ? System.currentTimeMillis() : event.getCreatedAt();
            RedisUtil.zadd(FEED_LATEST_KEY, score, event.getVideoId().toString());
            RedisUtil.zremrangeByRank(FEED_LATEST_KEY, 0, -MAX_LATEST_SIZE - 1L);
            RedisUtil.del(VIDEO_LIST_NEW_KEY);
            RedisUtil.del(VIDEO_DETAIL_PREFIX + event.getVideoId());
            log.info("视频发布 Feed 缓存刷新完成，eventId={}, videoId={}, key={}",
                    event.getEventId(), event.getVideoId(), FEED_LATEST_KEY);
        } catch (Exception e) {
            log.error("处理视频发布 Feed 缓存失败，eventId={}, videoId={}",
                    event.getEventId(), event.getVideoId(), e);
        }
    }
}
