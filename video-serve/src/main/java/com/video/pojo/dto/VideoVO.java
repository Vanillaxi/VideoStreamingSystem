package com.video.pojo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoVO {
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private Long userId;
    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private String videoUrl;
    private Long size;
    private String status;
    private Long likesCount;
    private Long commentCount;
    private Long favoriteCount;
    private Long viewCount;
    private Double hotScore;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
