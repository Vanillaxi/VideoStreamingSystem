package com.video.controller;

import com.video.annotation.MyMapping;
import com.video.messageQueue.kafka.KafkaTestProducer;
import com.video.pojo.dto.KafkaTestMessageRequest;
import com.video.pojo.dto.Result;
import jakarta.servlet.annotation.WebServlet;

/**
 * 用于测试Kafka
 */
@WebServlet("/test/kafka/*")
public class TestKafkaController extends BaseController {
    @MyMapping(value = "/send", method = "POST")
    public Result sendKafkaTestMessage(KafkaTestMessageRequest request) {
        String message = KafkaTestProducer.sendTestMessage(request);
        return Result.success(message);
    }
}
