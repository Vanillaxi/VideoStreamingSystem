package com.video.messageQueue.kafka;

import com.video.mapper.FollowMapper;
import com.video.pojo.dto.VideoPublishedEvent;
import com.video.pojo.entity.UserFollow;
import com.video.proxy.BeanFactory;
import com.video.utils.AppProperties;
import com.video.utils.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
final class VideoPublishedEventConsumerSupport {
    static final String DEFAULT_TOPIC = "video_published";
    static final String EVENT_TYPE = "FOLLOWING_VIDEO_PUBLISHED";

    private static final Properties CONFIG = AppProperties.getProperties();

    private VideoPublishedEventConsumerSupport() {
    }

    static void consumeLoop(String consumerName, AtomicBoolean started, String groupProperty,
                            String defaultGroupId, Consumer<VideoPublishedEvent> handler) {
        String topic = topic();
        try (KafkaConsumer<String, String> kafkaConsumer = createConsumer(groupProperty, defaultGroupId)) {
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            while (started.get()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    VideoPublishedEvent event = parseEvent(record);
                    if (event == null) {
                        continue;
                    }
                    try {
                        handler.accept(event);
                    } catch (Exception e) {
                        log.error("{} 处理消息失败，topic={}, partition={}, offset={}, eventId={}, videoId={}",
                                consumerName, record.topic(), record.partition(), record.offset(),
                                event.getEventId(), event.getVideoId(), e);
                    }
                }
                if (!records.isEmpty()) {
                    kafkaConsumer.commitSync();
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            if (started.get()) {
                log.error("{} 被异常唤醒", consumerName, e);
            }
        } catch (Exception e) {
            log.error("{} 运行失败", consumerName, e);
        }
    }

    static KafkaConsumer<String, String> createConsumer(String groupProperty, String defaultGroupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                CONFIG.getProperty("kafka.bootstrap.servers", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONFIG.getProperty(groupProperty, defaultGroupId));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new KafkaConsumer<>(props);
    }

    static String topic() {
        return CONFIG.getProperty("kafka.video.publish.topic", DEFAULT_TOPIC);
    }

    static VideoPublishedEvent parseEvent(ConsumerRecord<String, String> record) {
        try {
            VideoPublishedEvent event = JSONUtil.toBean(record.value(), VideoPublishedEvent.class);
            if (event == null || event.getVideoId() == null || event.getAuthorId() == null) {
                log.warn("忽略非法 video_published 消息，topic={}, partition={}, offset={}, value={}",
                        record.topic(), record.partition(), record.offset(), record.value());
                return null;
            }
            return event;
        } catch (Exception e) {
            log.error("解析 video_published 消息失败，topic={}, partition={}, offset={}, value={}",
                    record.topic(), record.partition(), record.offset(), record.value(), e);
            return null;
        }
    }

    static Set<Long> findFanIds(Long authorId) {
        FollowMapper followMapper = BeanFactory.getBean(FollowMapper.class);
        List<UserFollow> relations = followMapper.findFollowerRelations(authorId);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptySet();
        }
        return relations.stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static void shutdownExecutor(ExecutorService executorService) {
        if (executorService == null) {
            return;
        }
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
}
