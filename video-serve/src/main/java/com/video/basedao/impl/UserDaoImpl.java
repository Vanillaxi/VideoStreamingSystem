package com.video.basedao.impl;

import com.video.annotation.MyComponent;
import com.video.basedao.Dao; // 调用你写的工具类
import com.video.basedao.UserDao;
import com.video.entity.User;
import java.util.List;

@MyComponent
public class UserDaoImpl implements UserDao {

    @Override
    public int save(User user) {
        String sql = "INSERT INTO user(username, password, role) VALUES(?, ?, ?)";
        return Dao.executeUpdate(sql, user.getUsername(), user.getPassword(), user.getRole());
    }

    @Override
    public User getByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        List<User> list = Dao.executeQuery(User.class, sql, username);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

}