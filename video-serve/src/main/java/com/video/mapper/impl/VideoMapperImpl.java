package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.config.DBPool;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.pojo.entity.Video;
import com.video.mapper.VideoMapper;
import com.video.utils.XmlSqlReaderUtil;
import com.video.utils.JdbcUtils;
import java.util.Collections;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static com.video.utils.JdbcUtils.executeQuery;
import static com.video.utils.JdbcUtils.executeQueryCount;

@MyComponent
public class VideoMapperImpl implements VideoMapper {

    /**
     * 根据视频id查找
     * @param id
     * @return
     */
    @Override
    public Video getById(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getById");
        List<Video> list = JdbcUtils.executeQuery(Video.class, sql, id);

        if (list == null || list.isEmpty()) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }

        return list.get(0);
    }

    @Override
    public List<Video> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String sqlTemplate = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getByIds");
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format(sqlTemplate, placeholders);
        return JdbcUtils.executeQuery(Video.class, sql, ids.toArray());
    }

    /**
     * 发视频
     * @param video
     */
    @Override
    public void postVideo(Video video) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.postVideo");
        JdbcUtils.executeUpdate(sql,
                video.getTitle(),
                video.getDescription(),
                video.getCategoryId(),
                video.getUserId(),
                video.getVideoUrl(),
                video.getObjectKey(),
                video.getSize(),
                video.getStatus(),
                video.getLikesCount(),
                video.getCommentCount(),
                video.getFavoriteCount(),
                video.getViewCount(),
                video.getHotScore(),
                video.getCreateTime(),
                video.getUpdateTime());
    }

    @Override
    public void deleteById(Long videoId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.deleteById");
        JdbcUtils.executeUpdate(sql, videoId);
    }

    /**
     * 点赞
     * @param videoId
     * @param i
     */
    @Override
    public void updateLikes(Long videoId,int i) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateLikes");
        JdbcUtils.executeUpdate(sql,i,videoId);
    }

    @Override
    public int changeLikeWithTransaction(Long videoId, Long userId) {
        String existsSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.existsLike");
        String insertSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.insertLikes");
        String deleteSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.deleteLikes");
        String updateSql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateLikes");
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            boolean exists;
            try (PreparedStatement pstmt = conn.prepareStatement(existsSql)) {
                pstmt.setLong(1, videoId);
                pstmt.setLong(2, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    exists = rs.next();
                }
            }

            int delta;
            int rows;
            if (exists) {
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setLong(1, videoId);
                    pstmt.setLong(2, userId);
                    rows = pstmt.executeUpdate();
                }
                delta = rows > 0 ? -1 : 0;
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setLong(1, videoId);
                    pstmt.setLong(2, userId);
                    rows = pstmt.executeUpdate();
                }
                delta = rows > 0 ? 1 : 0;
            }

            if (delta != 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, delta);
                    pstmt.setLong(2, videoId);
                    pstmt.executeUpdate();
                }
            }
            conn.commit();
            return delta;
        } catch (Exception e) {
            rollback(conn);
            throw new SystemException(ErrorCode.LIKE_OPERATION_FAILED, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
    }

    @Override
    public void updateCommentCount(Long videoId, int count) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateCommentCount");
        JdbcUtils.executeUpdate(sql, count, videoId);
    }

    @Override
    public void updateFavoriteCount(Long videoId, int count) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateFavoriteCount");
        JdbcUtils.executeUpdate(sql, count, videoId);
    }

    @Override
    public int incrementViewCount(Long videoId, long increment) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.incrementViewCount");
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            int rows;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, increment);
                pstmt.setLong(2, videoId);
                rows = pstmt.executeUpdate();
            }
            conn.commit();
            return rows;
        } catch (Exception e) {
            rollback(conn);
            throw new SystemException(ErrorCode.VIEW_COUNT_FLUSH_FAILED, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
    }

    /**
     * 模糊查询
     * @param title
     * @return
     */
    @Override
    public List<Video> getVideoPageByTitle(String title, int offset, int pageSize, String sort) {
        String sql = XmlSqlReaderUtil.getSql(videoPageByTitleSqlId(sort));
        return executeQuery(Video.class, sql,title, offset, pageSize);
    }

    /**
     * 模糊查询数量
     * @param title
     * @return
     */
    @Override
    public Long getVideoCountByTitle(String title) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getVideoCountByTitle");
        return executeQueryCount( sql, title);
    }

    @Override
    public List<Video> getVideoPageByCategoryId(Long categoryId, int offset, int pageSize, String sort) {
        String sql = XmlSqlReaderUtil.getSql(videoPageByCategorySqlId(sort));
        return executeQuery(Video.class, sql, categoryId, offset, pageSize);
    }

    @Override
    public Long getVideoCountByCategoryId(Long categoryId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getVideoCountByCategoryId");
        return executeQueryCount(sql, categoryId);
    }

    @Override
    public List<Video> getHotTop50() {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getHotTop50");
        return executeQuery(Video.class, sql);
    }

    @Override
    public List<Video> getNewestPage(int offset, int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getNewestPage");
        return executeQuery(Video.class, sql, offset, pageSize);
    }

    @Override
    public Long getVideoCount() {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getVideoCount");
        return executeQueryCount(sql);
    }


    //在video_like 里面操作
    @Override
    public void insertLikes(Long videoId, Long userId) {
        String  sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.insertLikes");
        JdbcUtils.executeUpdate(sql,videoId,userId);
    }

    //在video_like 里面操作
    @Override
    public void deleteLikes(Long videoId, Long userId) {
        String  sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.deleteLikes");
        JdbcUtils.executeUpdate(sql,videoId,userId);
    }

    private String videoPageByTitleSqlId(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return "com.video.mapper.VideoMapper.getVideoPageByTitleHot";
        }
        return "com.video.mapper.VideoMapper.getVideoPageByTitle";
    }

    private String videoPageByCategorySqlId(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return "com.video.mapper.VideoMapper.getVideoPageByCategoryIdHot";
        }
        return "com.video.mapper.VideoMapper.getVideoPageByCategoryId";
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
