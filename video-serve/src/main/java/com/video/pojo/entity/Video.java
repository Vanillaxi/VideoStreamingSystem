package com.video.pojo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private Long userId;//博主Id
    private String videoUrl; // OSS 视频播放地址
    private String objectKey; // OSS 对象 key，用于删除文件
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
