package com.video.pojo.dto;

import lombok.Data;

@Data
public class RealtimeFeedMessage {
    private String type;
    private Long videoId;
    private Long authorId;
    private Long createdAt;
    private String content;
}
