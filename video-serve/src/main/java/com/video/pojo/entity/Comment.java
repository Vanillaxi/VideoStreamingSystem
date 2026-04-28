package com.video.pojo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Comment {
    private Long id;
    private Long userId;
    private Long videoId;
    private Long parentId;
    private Long rootId;
    private Long replyToUserId;
    private String content;
    private Long likesCount;
    private Long replyCount;
    private Double hotScore;
    private Integer deleted;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String username;
    private String nickname;
    private String replyToUsername;
    private String replyToNickname;
    private List<Comment> replies = new ArrayList<>();
}
