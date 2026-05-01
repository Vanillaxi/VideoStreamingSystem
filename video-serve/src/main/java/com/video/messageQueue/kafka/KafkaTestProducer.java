package com.video.messageQueue.kafka;

import com.video.pojo.dto.KafkaTestMessageRequest;
import com.video.utils.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class KafkaTestProducer {
    private static final String CONFIG_FILE = "properties/Kafka.properties";
    private static final String DEFAULT_TOPIC = "video_publish";
    private static final Properties config = loadConfig();
    private static volatile KafkaProducer<String, String> producer;

    private KafkaTestProducer() {
    }

    public static String sendTestMessage(KafkaTestMessageRequest request) {
        Long videoId = request == null || request.getVideoId() == null ? 1L : request.getVideoId();
        Long authorId = request == null || request.getAuthorId() == null ? 1L : request.getAuthorId();
        Long createdAt = request == null || request.getCreatedAt() == null ? System.currentTimeMillis() : request.getCreatedAt();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "VIDEO_PUBLISHED");
        event.put("videoId", videoId);
        event.put("authorId", authorId);
        event.put("createdAt", createdAt);

        String topic = config.getProperty("kafka.video.publish.topic", DEFAULT_TOPIC);
        String message = JSONUtil.toJson(event);
        getProducer().send(new ProducerRecord<>(topic, videoId.toString(), message), (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka 测试消息发送失败，topic={}, message={}", topic, message, exception);
            } else {
                log.info("Kafka 测试消息发送成功，topic={}, partition={}, offset={}, message={}",
                        metadata.topic(), metadata.partition(), metadata.offset(), message);
            }
        });
        getProducer().flush();
        return message;
    }

    public static void close() {
        KafkaProducer<String, String> current = producer;
        if (current != null) {
            current.close(Duration.ofSeconds(5));
            producer = null;
            log.info("Kafka Test Producer 已关闭");
        }
    }

    private static KafkaProducer<String, String> getProducer() {
        if (producer == null) {
            synchronized (KafkaTestProducer.class) {
                if (producer == null) {
                    Properties props = new Properties();
                    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                            config.getProperty("kafka.bootstrap.servers", "localhost:9092"));
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.ACKS_CONFIG, "all");
                    producer = new KafkaProducer<>(props);
                }
            }
        }
        return producer;
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (Exception e) {
            log.warn("读取 Kafka 测试配置失败，使用默认配置", e);
        }
        return properties;
    }
}
