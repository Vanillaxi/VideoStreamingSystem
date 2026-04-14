package com.video.basedao.impl;

import com.video.annotation.MyComponent;
import com.video.basedao.CommentDao;
import com.video.entity.Comment;
import java.util.List;
import static com.video.basedao.Dao.executeQuery;
import static com.video.basedao.Dao.executeUpdate;


@MyComponent
public class CommentDaoImpl implements CommentDao {

    @Override
    public int insert(Comment comment) {
        String sql = "insert into comments (video_id, user_id, content, create_time) values (?, ?, ?, ?) ";
        return executeUpdate(sql,
                comment.getVideoId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreateTime());
    }

    @Override
    public List<Comment> findByVideoId(Long videoId) {
        String sql = "SELECT c.id, c.video_id AS videoId, c.user_id AS userId, " +
                "c.content, c.create_time AS createTime, u.username " +
                "FROM comments c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.video_id = ? ORDER BY c.create_time DESC";

        return executeQuery(Comment.class, sql, videoId);
    }

    @Override
    public int deleteById(Long id) {
        String sql = "delete from comments where id = ?";
        return executeUpdate(sql, id);
    }
}