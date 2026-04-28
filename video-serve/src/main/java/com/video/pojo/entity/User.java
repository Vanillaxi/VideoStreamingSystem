package com.video.pojo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    @JSONField(serialize = false)
    private String password; // 存储加密后的哈希值
    private Integer role;    // 0:普通用户, 1:会员，2：普通管理员，3：Vanilla_xi
    private String nickname;
    private String avatarUrl;
    private String avatarObjectKey;

    private String creatUser;
    private String updateUser;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
