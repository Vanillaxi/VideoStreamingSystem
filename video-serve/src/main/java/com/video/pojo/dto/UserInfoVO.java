package com.video.pojo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long targetUserId;
    private String nickname;
    private String avatarUrl;
    private String avatar;
    private Integer role;
    private String bio;
    private String introduction;
    private String gender;
    private Long followCount;
    private Long fanCount;
    private Long mutualFollowCount;
    private Boolean isFollowed;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
