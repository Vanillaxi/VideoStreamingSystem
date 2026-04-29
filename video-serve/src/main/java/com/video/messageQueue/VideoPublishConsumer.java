package com.video.messageQueue;

import com.video.mapper.FollowMapper;
import com.video.pojo.dto.RealtimeFeedMessage;
import com.video.pojo.dto.VideoPublishedEvent;
import com.video.pojo.entity.UserFollow;
import com.video.proxy.BeanFactory;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil;
import com.video.websocket.NotificationWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class VideoPublishConsumer {
    private static final String CONFIG_FILE = "properties/Kafka.properties";
    private static final String DEFAULT_TOPIC = "video_publish";
    private static final String FEED_PREFIX = "feed:user:";
    private static final String FOLLOWER_PREFIX = "follower:";
    private static final String COMPAT_FOLLOWER_PREFIX = "user:followers:";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final Properties config = loadConfig();
    private static ExecutorService executorService;
    private static volatile KafkaConsumer<String, String> consumer;

    private VideoPublishConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("视频发布 Kafka Consumer 已启动，跳过重复启动");
            return;
        }
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "video-publish-consumer");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(VideoPublishConsumer::consumeLoop);
        log.info("视频发布 Kafka Consumer 启动成功");
    }

    public static void shutdown() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        KafkaConsumer<String, String> current = consumer;
        if (current != null) {
            current.wakeup();
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
            } finally {
                executorService = null;
            }
        }
        log.info("视频发布 Kafka Consumer 已关闭");
    }

    private static void consumeLoop() {
        String topic = config.getProperty("kafka.video.publish.topic", DEFAULT_TOPIC);
        try (KafkaConsumer<String, String> kafkaConsumer = createConsumer()) {
            consumer = kafkaConsumer;
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            while (STARTED.get()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
                if (!records.isEmpty()) {
                    kafkaConsumer.commitSync();
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            if (STARTED.get()) {
                log.error("视频发布 Kafka Consumer 被异常唤醒", e);
            }
        } catch (Exception e) {
            log.error("视频发布 Kafka Consumer 运行失败", e);
        } finally {
            consumer = null;
        }
    }

    private static void processRecord(ConsumerRecord<String, String> record) {
        try {
            VideoPublishedEvent event = JSONUtil.toBean(record.value(), VideoPublishedEvent.class);
            if (event == null || event.getVideoId() == null || event.getAuthorId() == null || event.getCreatedAt() == null) {
                log.warn("忽略非法视频发布消息，record={}", record.value());
                return;
            }

            Set<Long> fanIds = findFanIds(event.getAuthorId());
            if (fanIds.isEmpty()) {
                log.info("视频发布消息无粉丝需要推送，videoId={}, authorId={}", event.getVideoId(), event.getAuthorId());
                return;
            }

            Map<String, Double> feedWriteMap = new LinkedHashMap<>();
            for (Long fanId : fanIds) {
                feedWriteMap.put(FEED_PREFIX + fanId, event.getCreatedAt().doubleValue());
            }
            RedisUtil.zaddBatch(feedWriteMap, event.getVideoId().toString());
            pushRealtimeMessage(event, fanIds);
            log.info("视频发布 Feed 推送完成，videoId={}, authorId={}, fanCount={}",
                    event.getVideoId(), event.getAuthorId(), feedWriteMap.size());
        } catch (Exception e) {
            log.error("消费视频发布消息失败，topic={}, partition={}, offset={}, value={}",
                    record.topic(), record.partition(), record.offset(), record.value(), e);
            throw e;
        }
    }

    private static void pushRealtimeMessage(VideoPublishedEvent event, Set<Long> fanIds) {
        RealtimeFeedMessage message = new RealtimeFeedMessage();
        message.setType("NEW_VIDEO");
        message.setVideoId(event.getVideoId());
        message.setAuthorId(event.getAuthorId());
        message.setCreatedAt(event.getCreatedAt());
        message.setContent("你关注的用户发布了新视频");
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
        if (onlineCount > 0) {
            log.info("WebSocket 实时 Feed 推送完成，videoId={}, onlineFans={}, success={}",
                    event.getVideoId(), onlineCount, successCount);
        }
    }

    private static Set<Long> findFanIds(Long authorId) {
        Set<Long> fanIds = new LinkedHashSet<>();
        addFanIdsFromRedis(fanIds, FOLLOWER_PREFIX + authorId);
        addFanIdsFromRedis(fanIds, COMPAT_FOLLOWER_PREFIX + authorId);
        if (!fanIds.isEmpty()) {
            return fanIds;
        }

        FollowMapper followMapper = BeanFactory.getBean(FollowMapper.class);
        List<UserFollow> relations = followMapper.findFollowerRelations(authorId);
        fanIds.addAll(relations.stream().map(UserFollow::getFollowerId).collect(Collectors.toList()));
        return fanIds;
    }

    private static void addFanIdsFromRedis(Set<Long> fanIds, String key) {
        List<String> members = RedisUtil.zrevrange(key, 0, -1);
        if (members == null || members.isEmpty()) {
            return;
        }
        for (String member : members) {
            try {
                fanIds.add(Long.valueOf(member));
            } catch (NumberFormatException e) {
                log.warn("粉丝 Redis member 解析失败，key={}, member={}", key, member, e);
            }
        }
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.getProperty("kafka.bootstrap.servers", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                config.getProperty("kafka.video.publish.group.id", "video-feed-push-group"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new KafkaConsumer<>(props);
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (Exception e) {
            log.warn("读取 Kafka 配置失败，使用默认配置", e);
        }
        return properties;
    }
}
