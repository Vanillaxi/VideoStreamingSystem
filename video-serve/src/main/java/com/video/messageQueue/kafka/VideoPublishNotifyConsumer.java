package com.video.messageQueue.kafka;

import com.video.pojo.dto.RealtimeFeedMessage;
import com.video.pojo.dto.VideoPublishedEvent;
import com.video.utils.JSONUtil;
import com.video.websocket.NotificationWebSocketServer;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VideoPublishNotifyConsumer {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ExecutorService executorService;

    private VideoPublishNotifyConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("视频发布 WebSocket 通知 Consumer 已启动，跳过重复启动");
            return;
        }
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "video-publish-notify-consumer");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(() -> VideoPublishedEventConsumerSupport.consumeLoop(
                "视频发布 WebSocket 通知 Consumer",
                STARTED,
                "kafka.video.notify.group.id",
                "video-publish-notify-group",
                VideoPublishNotifyConsumer::handleEvent
        ));
        log.info("视频发布 WebSocket 通知 Consumer 启动成功");
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        VideoPublishedEventConsumerSupport.shutdownExecutor(executorService);
        executorService = null;
        log.info("视频发布 WebSocket 通知 Consumer 已关闭");
    }

    private static void handleEvent(VideoPublishedEvent event) {
        try {
            Set<Long> fanIds = VideoPublishedEventConsumerSupport.findFanIds(event.getAuthorId());
            if (fanIds.isEmpty()) {
                log.info("视频发布事件无粉丝在线推送目标，eventId={}, videoId={}", event.getEventId(), event.getVideoId());
                return;
            }

            RealtimeFeedMessage message = new RealtimeFeedMessage();
            message.setType(VideoPublishedEventConsumerSupport.EVENT_TYPE);
            message.setVideoId(event.getVideoId());
            message.setAuthorId(event.getAuthorId());
            message.setTitle(event.getTitle());
            message.setCoverUrl(event.getCoverUrl());
            message.setCreateTime(event.getCreateTime());
            message.setCreatedAt(event.getCreatedAt());
            message.setContent("你关注的人发布了新视频");
            String json = JSONUtil.toJson(message);

            int onlineCount = 0;
            int successCount = 0;
            for (Long fanId : fanIds) {
                if (!NotificationWebSocketServer.isOnline(fanId)) {
                    continue;
                }
                onlineCount++;
                if (NotificationWebSocketServer.sendToUser(fanId, json)) {
                    successCount++;
                }
            }
            log.info("视频发布 WebSocket 通知完成，eventId={}, videoId={}, onlineFans={}, success={}",
                    event.getEventId(), event.getVideoId(), onlineCount, successCount);
        } catch (Exception e) {
            log.error("处理视频发布 WebSocket 通知失败，eventId={}, videoId={}",
                    event.getEventId(), event.getVideoId(), e);
        }
    }
}
