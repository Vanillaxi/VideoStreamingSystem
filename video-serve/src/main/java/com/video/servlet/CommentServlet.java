package com.video.servlet;

import com.video.config.BeanFactory;
import com.video.service.CommentService;
import com.video.result.Result;
import com.video.utils.JSONUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/comment/*")
public class CommentServlet extends HttpServlet {

    private CommentService commentService = BeanFactory.getBean(CommentService.class);

    /**
     * 查看评论
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/list".equals(path)) {
            Long videoId = Long.parseLong(req.getParameter("videoId"));
            var list = commentService.getCommentsByVideoId(videoId);
            resp.getWriter().write(JSONUtil.toJson(Result.success(list)));
        }
    }

    /**
     * 发布评论
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/add".equals(path)) {
            Long videoId = Long.parseLong(req.getParameter("videoId"));
            String content = req.getParameter("content");

            boolean success = commentService.postComment(videoId, content);
            if (success) {
                resp.getWriter().write(JSONUtil.toJson(Result.success("评论成功")));
            } else {
                resp.getWriter().write(JSONUtil.toJson(Result.error("评论失败")));
            }
        }
    }
}