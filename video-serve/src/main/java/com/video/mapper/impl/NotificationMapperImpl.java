package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.mapper.NotificationMapper;
import com.video.pojo.entity.Notification;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

@MyComponent
public class NotificationMapperImpl implements NotificationMapper {
    @Override
    public void insert(Notification notification) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.NotificationMapper.insert");
        JdbcUtils.executeUpdate(sql,
                notification.getUserId(),
                notification.getType(),
                notification.getContent(),
                notification.getRelatedId(),
                notification.getIsRead(),
                notification.getCreateTime());
    }
}
