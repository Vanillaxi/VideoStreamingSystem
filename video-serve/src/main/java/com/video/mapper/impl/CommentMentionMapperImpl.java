package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.mapper.CommentMentionMapper;
import com.video.pojo.entity.CommentMention;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

@MyComponent
public class CommentMentionMapperImpl implements CommentMentionMapper {
    @Override
    public void insert(CommentMention mention) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMentionMapper.insert");
        JdbcUtils.executeUpdate(sql,
                mention.getCommentId(),
                mention.getFromUserId(),
                mention.getToUserId(),
                mention.getIsRead(),
                mention.getCreateTime());
    }
}
