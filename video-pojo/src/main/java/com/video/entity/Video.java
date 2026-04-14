package com.video.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.annotation.JSONField;

import java.time.LocalDateTime;

@Data
public class Video {
    private Long id;
    private String title;
    private String url; // 视频播放路径
    private String cover;//封面图
    private Long userId;//博主Id

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
