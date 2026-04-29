package com.video.controller;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.PageResult;
import com.video.service.CommentService;
import com.video.pojo.dto.Result;
import jakarta.servlet.annotation.WebServlet;


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
    public Result getCommentsByVideoId(Long videoId, int page, int pageSize, String sort){
        PageResult comments = commentService.getCommentsByVideoId(videoId,page,pageSize,sort);
        return Result.success(comments);
    }

    /**
     * 评论游标分页。sort=time/hot；第一页不传游标，后续传上一页返回的游标。
     */
    @MyMapping(value="/list/cursor",method = "GET")
    public Result getCommentsByVideoIdCursor(Long videoId, String sort, Double cursorHotScore,
                                             String cursorCreateTime, Long cursorId, Integer pageSize) {
        return Result.success(commentService.getCommentsByVideoIdCursor(videoId, sort, cursorHotScore,
                cursorCreateTime, cursorId, pageSize));
    }

    /**
     * 发布评论
     * @param videoId
     * @param content
     * @return
     */
    @MyMapping(value="/add",method = "POST")
    public Result addComments(Long videoId, Long parentId, String content){
        commentService.addComment(videoId, parentId, content);
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
