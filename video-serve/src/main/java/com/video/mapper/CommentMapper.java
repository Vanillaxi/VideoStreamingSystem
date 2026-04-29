package com.video.mapper;

import com.video.pojo.entity.Comment;
import java.util.List;


public interface CommentMapper {
    //新增
    void insert( Comment comment);

    //根据id查询
    Comment findByCommentId(Long commentId);

    //删除
    void delete (Long commentId);


    //根据videoId分页查询
    List<Comment> findPageByVideoId(Long videoId,int offset, int pageSize, String sort);
    List<Comment> findCursorPageByVideoIdTime(Long videoId, java.time.LocalDateTime cursorCreateTime, Long cursorId, int limit);
    List<Comment> findCursorPageByVideoIdHot(Long videoId, Double cursorHotScore, java.time.LocalDateTime cursorCreateTime, Long cursorId, int limit);
    Long countByVideoId(Long videoId);
    List<Comment> findRepliesByRootIds(List<Long> rootIds);

    void updateLikesCount(Long commentId, int i);
}
