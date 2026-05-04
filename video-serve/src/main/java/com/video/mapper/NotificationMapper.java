package com.video.mapper;

import com.video.pojo.entity.Notification;

public interface NotificationMapper {
    void insert(Notification notification);

    boolean exists(Long userId, String type, Long relatedId);
}
