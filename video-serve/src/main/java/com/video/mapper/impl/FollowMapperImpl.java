package com.video.mapper.impl;


import com.video.annotation.MyComponent;
import com.video.pojo.entity.User;
import com.video.mapper.FollowMapper;
import com.video.utils.XmlSqlReaderUtil;
import java.util.List;
import static com.video.utils.JdbcUtils.*;

@MyComponent
public class FollowMapperImpl implements FollowMapper {

    //我的关注
    @Override
    public List<User> findFollowings(Long userId,int offset,int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFollowings");
        return executeQuery(User.class, sql, userId, offset, pageSize);
    }

    //我的粉丝
    @Override
    public List<User> findFollowers(Long userId,int offset,int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFollowers");
        return executeQuery(User.class, sql, userId, offset, pageSize);
    }


    //我的互关
    @Override
    public List<User> findFriends(Long userId,int offset,int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFriends");
        return executeQuery(User.class, sql, userId);
    }


    /**
     * 查看是否关注
     * @param followingId
     * @return
     */
    @Override
    public Boolean isFollow(Long followingId,Long followerId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.isFollow");
        return executeExists( sql,followingId,followerId);
    }

    //关注
    @Override
    public Long follow(Long followingId, Long followerId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.follow");
        return (long) executeUpdate( sql,followingId,followerId);
    }

    //取关
    @Override
    public Long unFollow(Long followingId, Long followerId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.unFollow");
        return (long) executeUpdate( sql,followingId,followerId);
    }


    @Override
    public Long countFollowings(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.countFollowings");
        return executeQueryCount( sql, userId);
    }

    @Override
    public Long countFollowers(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.countFollowers");
        return executeQueryCount( sql, userId);
    }

    @Override
    public Long countFriends(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.countFriends");
        return executeQueryCount( sql, userId);
    }

}
