package com.video.pojo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentMention {
    private Long id;
    private Long commentId;
    private Long fromUserId;
    private Long toUserId;
    private Integer isRead;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
