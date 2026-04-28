package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.config.DBPool;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.pojo.entity.Comment;
import com.video.mapper.CommentMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import com.video.utils.XmlSqlReaderUtil;
import static com.video.utils.JdbcUtils.*;

@MyComponent
public class CommentMapperImpl implements CommentMapper {

    //发表评论
    @Override
    public void insert(Comment comment) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.insert");
        String updateRootSql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.updateRootId");
        String findParentSql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.findParentForUpdate");
        String updateVideoCommentCountSql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.updateVideoCommentCount");
        String updateReplyCountSql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.updateReplyCount");
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);

            if (comment.getParentId() != null) {
                Comment parent = findParentForUpdate(conn, findParentSql, comment.getParentId());
                if (parent == null) {
                    throw new BusinessException(ErrorCode.PARENT_COMMENT_NOT_FOUND);
                }
                if (!parent.getVideoId().equals(comment.getVideoId())) {
                    throw new BusinessException(ErrorCode.PARENT_COMMENT_NOT_FOUND);
                }
                comment.setRootId(parent.getRootId() == null ? parent.getId() : parent.getRootId());
                comment.setReplyToUserId(parent.getUserId());
            }

            Long commentId = insertComment(conn, sql, comment);
            comment.setId(commentId);

            if (comment.getParentId() == null) {
                comment.setRootId(commentId);
                try (PreparedStatement pstmt = conn.prepareStatement(updateRootSql)) {
                    pstmt.setLong(1, commentId);
                    pstmt.setLong(2, commentId);
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(updateReplyCountSql)) {
                    pstmt.setLong(1, comment.getParentId());
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateVideoCommentCountSql)) {
                pstmt.setLong(1, comment.getVideoId());
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } catch (Exception e) {
            rollback(conn);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
    }

    //查看评论
    @Override
    public Comment findByCommentId(Long commentId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.findByCommentId");
        List<Comment> commentList = executeQuery(Comment.class, sql, commentId);
        if (commentList == null || commentList.size() == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return commentList.get(0);
    }

    //删除评论
    @Override
    public void delete(Long commentId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.delete");
        executeUpdate(sql, commentId);
    }

    //点赞
    @Override
    public void updateLikesCount(Long commentId, int i) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.updateLikesCount");
        executeUpdate(sql, i, commentId);
    }


    //分页查询
    @Override
    public List<Comment> findPageByVideoId(Long videoId, int offset, int pageSize, String sort) {
        String sql = XmlSqlReaderUtil.getSql(commentPageSqlId(sort));
        return executeQuery(Comment.class, sql, videoId, offset, pageSize);
    }

    @Override
    public List<Comment> findRepliesByRootIds(List<Long> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sqlTemplate = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.findRepliesByRootIds");
        String placeholders = String.join(",", Collections.nCopies(rootIds.size(), "?"));
        String sql = String.format(sqlTemplate, placeholders);
        return executeQuery(Comment.class, sql, rootIds.toArray());
    }

    @Override
    public Long countByVideoId(Long videoId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.countByVideoId");
        return executeQueryCount(sql, videoId);
    }

    private Long insertComment(Connection conn, String sql, Comment comment) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, comment.getVideoId());
            pstmt.setObject(2, comment.getUserId());
            pstmt.setObject(3, comment.getParentId());
            pstmt.setObject(4, comment.getRootId());
            pstmt.setObject(5, comment.getReplyToUserId());
            pstmt.setObject(6, comment.getContent());
            pstmt.setObject(7, Timestamp.valueOf(comment.getCreateTime()));
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("新增评论未返回主键");
    }

    private Comment findParentForUpdate(Connection conn, String sql, Long parentId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, parentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Comment comment = new Comment();
                comment.setId(rs.getLong("id"));
                comment.setVideoId(rs.getLong("videoId"));
                comment.setUserId(rs.getLong("userId"));
                comment.setParentId(getNullableLong(rs, "parentId"));
                comment.setRootId(getNullableLong(rs, "rootId"));
                comment.setReplyToUserId(getNullableLong(rs, "replyToUserId"));
                comment.setContent(rs.getString("content"));
                Timestamp createTime = rs.getTimestamp("createTime");
                comment.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
                comment.setLikesCount(rs.getLong("likesCount"));
                comment.setReplyCount(rs.getLong("replyCount"));
                comment.setHotScore(rs.getDouble("hotScore"));
                comment.setDeleted(rs.getInt("deleted"));
                return comment;
            }
        }
    }

    private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
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

    private String commentPageSqlId(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return "com.video.mapper.CommentMapper.findPageByVideoIdHot";
        }
        return "com.video.mapper.CommentMapper.findPageByVideoId";
    }

}
