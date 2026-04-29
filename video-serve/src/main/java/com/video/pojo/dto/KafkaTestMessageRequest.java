package com.video.pojo.dto;

import lombok.Data;

@Data
public class KafkaTestMessageRequest {
    private Long videoId;
    private Long authorId;
    private Long createdAt;
}
