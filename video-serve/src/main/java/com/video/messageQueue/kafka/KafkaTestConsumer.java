package com.video.messageQueue.kafka;

import com.video.utils.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class KafkaTestConsumer {
    private static final String DEFAULT_TOPIC = "video_published";
    private static final String TEST_GROUP_ID = "video-kafka-test-group";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final Properties config = loadConfig();
    private static ExecutorService executorService;
    private static volatile KafkaConsumer<String, String> consumer;

    private KafkaTestConsumer() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            log.info("Kafka 测试 Consumer 已启动，跳过重复启动");
            return;
        }
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "kafka-test-consumer");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(KafkaTestConsumer::consumeLoop);
        log.info("Kafka 测试 Consumer 启动成功");
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
        log.info("Kafka 测试 Consumer 已关闭");
    }

    private static void consumeLoop() {
        String topic = config.getProperty("kafka.video.publish.topic", DEFAULT_TOPIC);
        try (KafkaConsumer<String, String> kafkaConsumer = createConsumer()) {
            consumer = kafkaConsumer;
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            while (STARTED.get()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Kafka 测试 Consumer 收到完整消息：topic={}, partition={}, offset={}, key={}, value={}",
                            record.topic(), record.partition(), record.offset(), record.key(), record.value());
                    log.info("Kafka 消费成功");
                }
                if (!records.isEmpty()) {
                    kafkaConsumer.commitSync();
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            if (STARTED.get()) {
                log.error("Kafka 测试 Consumer 被异常唤醒", e);
            }
        } catch (Exception e) {
            log.error("Kafka 测试 Consumer 运行失败", e);
        } finally {
            consumer = null;
        }
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.getProperty("kafka.bootstrap.servers", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                config.getProperty("kafka.test.group.id", TEST_GROUP_ID));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new KafkaConsumer<>(props);
    }

    private static Properties loadConfig() {
        return AppProperties.getProperties();
    }
}
