package com.video.pojo.dto;

import lombok.Data;

@Data
public class VideoPublishedEvent {
    private String eventId;
    private Long videoId;
    private Long authorId;
    private String title;
    private String coverUrl;
    private String createTime;
    private Long eventTime;
    private Long createdAt;
    private Double hotScore;
    private String eventType;
}
