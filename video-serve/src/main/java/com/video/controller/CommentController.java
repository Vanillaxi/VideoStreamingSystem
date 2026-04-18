package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.entity.Comment;
import com.video.pojo.dto.PageResult;
import com.video.proxy.BeanFactory;
import com.video.service.CommentService;
import com.video.pojo.dto.Result;
import com.video.service.VideoService;
import com.video.utils.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/comment/*")
public class CommentController extends BaseController {
    @MyAutowired
    private CommentService commentService;

    /**
     * 对指定的视频id进行comment的分页查询
     * @param videoId
     * @return
     */
    @MyMapping(value="/list",method = "GET")
    public Result getCommentsByVideoId(Long videoId, int page, int pageSize){
        PageResult comments = commentService.getCommentsByVideoId(videoId,page,pageSize);
        return Result.success(comments);
    }

    /**
     * 发布评论
     * @param videoId
     * @param content
     * @return
     */
    @MyMapping(value="/add",method = "POST")
    public Result addComments(Long videoId,String content){
        commentService.addComment(videoId, content);
        return Result.success("发表成功");
    }

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    @MyMapping(value="/delete",method = "DELETE")
    public Result deleteComment(Long commentId){
        commentService.delete(commentId);
        return Result.success("删除成功");
    }

    /**
     * 更改点赞
     * @param commentId
     * @return
     */
    @MyMapping(value="/update",method="POST")
    public Result updateLikesComment(Long commentId){
        commentService.updateLikesComment(commentId);
        return Result.success("操作成功");
    }

}