package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.exception.CommentNotFoundException;
import com.video.pojo.entity.Comment;
import com.video.mapper.CommentMapper;
import java.util.List;
import com.video.utils.XmlSqlReaderUtil;
import static com.video.utils.JdbcUtils.*;

@MyComponent
public class CommentMapperImpl implements CommentMapper {

    //发表评论
    @Override
    public void insert(Comment comment) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.insert");
        executeUpdate(sql, comment.getVideoId(), comment.getUserId(), comment.getContent(), comment.getCreateTime());
    }

    //查看评论
    @Override
    public Comment findByCommentId(Long commentId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.findByCommentId");
        List<Comment> commentList = executeQuery(Comment.class, sql, commentId);
        if (commentList == null || commentList.size() == 0) {
            throw new CommentNotFoundException();
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
    public List<Comment> findPageByVideoId(Long videoId, int offset, int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.findPageByVideoId");
        return executeQuery(Comment.class, sql, videoId, offset, pageSize);
    }

    @Override
    public Long countByVideoId(Long videoId) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CommentMapper.countByVideoId");
        return executeQueryCount(sql, videoId);
    }


}