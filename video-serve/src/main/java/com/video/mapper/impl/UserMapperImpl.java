package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.pojo.entity.User;
import com.video.mapper.UserMapper;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;
import java.util.Collections;
import java.util.List;

import static com.video.utils.JdbcUtils.executeUpdate;

@MyComponent
public class UserMapperImpl implements UserMapper {

    @Override
    public void insert(User user) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.insert");
        JdbcUtils.executeUpdate(sql,
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getAvatarObjectKey(),
                user.getCreatUser(),
                user.getUpdateUser());
    }

    @Override
    public User getByUsername(String username) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.getByUsername");
        List<User> userList = JdbcUtils.executeQuery(User.class, sql, username);
        if(userList.isEmpty()||userList==null){
            return null;
        }
        return userList.get(0);
    }

    @Override
    public User getByUserId(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.getByUserId");
        List<User> userList= JdbcUtils.executeQuery(User.class, sql, userId);

        if (userList == null || userList.isEmpty()) {
            return null;
        }

        return userList.get(0);
    }

    @Override
    public List<User> getByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sqlTemplate = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.getByIds");
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = String.format(sqlTemplate, placeholders);
        return JdbcUtils.executeQuery(User.class, sql, userIds.toArray());
    }

    @Override
    public List<User> getByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyList();
        }
        String sqlTemplate = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.getByUsernames");
        String placeholders = String.join(",", Collections.nCopies(usernames.size(), "?"));
        String sql = String.format(sqlTemplate, placeholders);
        return JdbcUtils.executeQuery(User.class, sql, usernames.toArray());
    }

    @Override
    public void update(User user) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.update");
        executeUpdate(sql,
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getAvatarObjectKey(),
                user.getRole(),
                user.getUpdateUser(),
                user.getId());
    }

    @Override
    public void delete(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.delete");
        executeUpdate(sql, userId);
    }
}
