package com.video.pojo.dto;

import lombok.Data;

@Data
public class VideoPublishedEvent {
    private String eventId;
    private Long videoId;
    private Long authorId;
    private Long createdAt;
    private Double hotScore;
    private String eventType;
}
