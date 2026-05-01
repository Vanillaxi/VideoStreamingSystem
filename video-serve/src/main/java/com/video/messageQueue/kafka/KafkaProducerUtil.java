package com.video.messageQueue.kafka;

import com.video.pojo.dto.VideoPublishedEvent;
import com.video.utils.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

@Slf4j
public class KafkaProducerUtil {
    private static final String CONFIG_FILE = "properties/Kafka.properties";
    private static final String DEFAULT_TOPIC = "video_publish";
    private static volatile KafkaProducer<String, String> producer;
    private static final Properties config = loadConfig();

    private KafkaProducerUtil() {
    }

    public static void sendVideoPublished(VideoPublishedEvent event) {
        String topic = config.getProperty("kafka.video.publish.topic", DEFAULT_TOPIC);
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    event.getVideoId().toString(),
                    JSONUtil.toJson(event)
            );
            getProducer().send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("发送视频发布 Kafka 消息失败，eventId={}, videoId={}",
                            event.getEventId(), event.getVideoId(), exception);
                } else {
                    log.info("发送视频发布 Kafka 消息成功，topic={}, partition={}, offset={}, videoId={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), event.getVideoId());
                }
            });
        } catch (Exception e) {
            log.error("发送视频发布 Kafka 消息异常，eventId={}, videoId={}", event.getEventId(), event.getVideoId(), e);
        }
    }

    public static void close() {
        KafkaProducer<String, String> current = producer;
        if (current != null) {
            current.close(Duration.ofSeconds(5));
            producer = null;
            log.info("Kafka Producer 已关闭");
        }
    }

    private static KafkaProducer<String, String> getProducer() {
        if (producer == null) {
            synchronized (KafkaProducerUtil.class) {
                if (producer == null) {
                    Properties props = new Properties();
                    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                            config.getProperty("kafka.bootstrap.servers", "localhost:9092"));
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.ACKS_CONFIG, "all");
                    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
                    props.put(ProducerConfig.RETRIES_CONFIG, "3");
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
            log.warn("读取 Kafka 配置失败，使用默认配置", e);
        }
        return properties;
    }
}
