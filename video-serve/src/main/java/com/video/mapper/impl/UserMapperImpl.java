package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.exception.AccountNotFoundException;
import com.video.pojo.entity.User;
import com.video.mapper.UserMapper;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;
import java.util.List;

import static com.video.utils.JdbcUtils.executeUpdate;

@MyComponent
public class UserMapperImpl implements UserMapper {

    @Override
    public void insert(User user) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.insert");
        JdbcUtils.executeUpdate(sql, user.getUsername(), user.getPassword(), user.getRole(),user.getNickname(),user.getCreatUser(),user.getUpdateUser());
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
    public void update(User user) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.update");
        executeUpdate(sql,user.getUsername(), user.getPassword(), user.getNickname(),user.getRole(), user.getUpdateUser(), user.getId());
    }

    @Override
    public void delete(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.UserMapper.delete");
        executeUpdate(sql, userId);
    }
}