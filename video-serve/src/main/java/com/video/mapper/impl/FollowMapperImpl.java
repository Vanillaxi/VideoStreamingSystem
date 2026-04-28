package com.video.mapper.impl;


import com.video.annotation.MyComponent;
import com.video.config.DBPool;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.pojo.entity.User;
import com.video.pojo.entity.UserFollow;
import com.video.mapper.FollowMapper;
import com.video.utils.XmlSqlReaderUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    public int changeFollowWithTransaction(Long followingId, Long followerId) {
        String existsSql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.isFollow");
        String followSql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.follow");
        String unFollowSql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.unFollow");
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            boolean exists;
            try (PreparedStatement pstmt = conn.prepareStatement(existsSql)) {
                pstmt.setLong(1, followingId);
                pstmt.setLong(2, followerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    exists = rs.next();
                }
            }

            int rows;
            int delta;
            if (exists) {
                try (PreparedStatement pstmt = conn.prepareStatement(unFollowSql)) {
                    pstmt.setLong(1, followingId);
                    pstmt.setLong(2, followerId);
                    rows = pstmt.executeUpdate();
                }
                delta = rows > 0 ? -1 : 0;
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(followSql)) {
                    pstmt.setLong(1, followingId);
                    pstmt.setLong(2, followerId);
                    rows = pstmt.executeUpdate();
                }
                delta = rows > 0 ? 1 : 0;
            }
            conn.commit();
            return delta;
        } catch (Exception e) {
            rollback(conn);
            throw new SystemException(ErrorCode.FOLLOW_OPERATION_FAILED, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
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

    @Override
    public List<UserFollow> findFollowingRelations(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFollowingRelations");
        return executeQuery(UserFollow.class, sql, userId);
    }

    @Override
    public List<UserFollow> findFollowerRelations(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFollowerRelations");
        return executeQuery(UserFollow.class, sql, userId);
    }

    @Override
    public List<UserFollow> findFriendRelations(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FollowMapper.findFriendRelations");
        return executeQuery(UserFollow.class, sql, userId);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ignored) {
        }
    }

    private void resetAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (Exception ignored) {
        }
    }

}
