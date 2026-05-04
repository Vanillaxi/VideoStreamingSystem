package com.video.messageQueue.kafka;

import com.video.mapper.NotificationMapper;
import com.video.pojo.dto.VideoPublishedEvent;
import com.video.pojo.entity.Notification;
import com.video.proxy.BeanFactory;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VideoPublishNotificationConsumer {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ExecutorService executorService;

    private VideoPublishNotificationConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("视频发布站内通知 Consumer 已启动，跳过重复启动");
            return;
        }
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "video-publish-notification-consumer");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(() -> VideoPublishedEventConsumerSupport.consumeLoop(
                "视频发布站内通知 Consumer",
                STARTED,
                "kafka.video.notification.group.id",
                "video-publish-notification-group",
                VideoPublishNotificationConsumer::handleEvent
        ));
        log.info("视频发布站内通知 Consumer 启动成功");
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        VideoPublishedEventConsumerSupport.shutdownExecutor(executorService);
        executorService = null;
        log.info("视频发布站内通知 Consumer 已关闭");
    }

    private static void handleEvent(VideoPublishedEvent event) {
        try {
            Set<Long> fanIds = VideoPublishedEventConsumerSupport.findFanIds(event.getAuthorId());
            if (fanIds.isEmpty()) {
                log.info("视频发布事件无站内通知接收人，eventId={}, videoId={}", event.getEventId(), event.getVideoId());
                return;
            }

            NotificationMapper notificationMapper = BeanFactory.getBean(NotificationMapper.class);
            int inserted = 0;
            for (Long fanId : fanIds) {
                if (notificationMapper.exists(fanId, VideoPublishedEventConsumerSupport.EVENT_TYPE, event.getVideoId())) {
                    continue;
                }
                Notification notification = new Notification();
                notification.setUserId(fanId);
                notification.setType(VideoPublishedEventConsumerSupport.EVENT_TYPE);
                notification.setContent("你关注的用户发布了新视频：《" + safeTitle(event.getTitle()) + "》");
                notification.setRelatedId(event.getVideoId());
                notification.setIsRead(0);
                notification.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(notification);
                inserted++;
            }
            log.info("视频发布站内通知写入完成，eventId={}, videoId={}, fanCount={}, inserted={}",
                    event.getEventId(), event.getVideoId(), fanIds.size(), inserted);
        } catch (Exception e) {
            log.error("处理视频发布站内通知失败，eventId={}, videoId={}", event.getEventId(), event.getVideoId(), e);
        }
    }

    private static String safeTitle(String title) {
        return title == null || title.isBlank() ? "未命名视频" : title;
    }
}
