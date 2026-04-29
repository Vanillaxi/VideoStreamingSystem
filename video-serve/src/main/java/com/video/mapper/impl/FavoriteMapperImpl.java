package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.config.DBPool;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.mapper.FavoriteMapper;
import com.video.pojo.entity.VideoFavorite;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;

@MyComponent
public class FavoriteMapperImpl implements FavoriteMapper {
    @Override
    public int insertFavorite(Long videoId, Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.insertFavorite");
        return JdbcUtils.executeUpdate(sql, videoId, userId);
    }

    @Override
    public int insertFavoriteWithTransaction(Long videoId, Long userId) {
        String insertSql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.insertFavorite");
        String updateCountSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateFavoriteCount");
        return changeFavoriteWithTransaction(videoId, userId, insertSql, updateCountSql, 1);
    }

    @Override
    public int deleteFavorite(Long videoId, Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.deleteFavorite");
        return JdbcUtils.executeUpdate(sql, videoId, userId);
    }

    @Override
    public int deleteFavoriteWithTransaction(Long videoId, Long userId) {
        String deleteSql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.deleteFavorite");
        String updateCountSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateFavoriteCount");
        return changeFavoriteWithTransaction(videoId, userId, deleteSql, updateCountSql, -1);
    }

    @Override
    public boolean existsFavorite(Long videoId, Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.existsFavorite");
        return JdbcUtils.executeExists(sql, videoId, userId);
    }

    @Override
    public List<VideoFavorite> findByUserId(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.findByUserId");
        return JdbcUtils.executeQuery(VideoFavorite.class, sql, userId);
    }

    @Override
    public Long countByUserId(Long userId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.FavoriteMapper.countByUserId");
        return JdbcUtils.executeQueryCount(sql, userId);
    }

    private int changeFavoriteWithTransaction(Long videoId, Long userId, String changeSql, String updateCountSql, int delta) {
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            int rows;
            try (PreparedStatement pstmt = conn.prepareStatement(changeSql)) {
                pstmt.setLong(1, videoId);
                pstmt.setLong(2, userId);
                rows = pstmt.executeUpdate();
            }
            if (rows > 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateCountSql)) {
                    pstmt.setInt(1, delta);
                    pstmt.setLong(2, videoId);
                    pstmt.executeUpdate();
                }
            }
            conn.commit();
            return rows;
        } catch (Exception e) {
            rollback(conn);
            throw new SystemException(delta > 0
                    ? ErrorCode.FAVORITE_OPERATION_FAILED
                    : ErrorCode.CANCEL_FAVORITE_FAILED, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
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
