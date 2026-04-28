package com.video.task;

import com.video.proxy.BeanFactory;
import com.video.service.VideoService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VideoViewCountFlushTask {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ScheduledExecutorService executorService;

    private VideoViewCountFlushTask() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("视频播放量刷库定时任务已启动，跳过重复启动");
            return;
        }

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "video-view-count-flush-task");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            try {
                VideoService videoService = BeanFactory.getBean(VideoService.class);
                videoService.flushViewCountToDb();
            } catch (Exception e) {
                log.error("视频播放量刷库定时任务执行失败", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        log.info("视频播放量刷库定时任务启动成功，执行间隔 1 分钟");
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("视频播放量刷库定时任务已关闭");
    }
}
