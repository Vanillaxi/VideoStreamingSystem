package com.video.basedao;

import com.video.entity.Comment;
import java.util.List;


public interface CommentDao {
    //新增
    int insert(Comment comment);

    //查询
    List<Comment> findByVideoId(Long videoId);

    //删除
    int deleteById(Long id);

}
