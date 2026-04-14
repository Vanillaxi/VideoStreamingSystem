package com.video.servlet;

import com.video.config.BeanFactory;
import com.video.entity.Video;
import com.video.result.Result;
import com.video.service.UserService;
import com.video.service.VideoService;
import com.video.utils.JSONUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
@WebServlet("/video/get")
public class VideoServlet extends HttpServlet {

    //从BeanFactory 获取代理后的Service （支持AOP日志）
    private VideoService videoService= BeanFactory.getBean(VideoService.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=utf-8");
        try {
            String idStr = req.getParameter("id");
            Long id = Long.parseLong(idStr);
            Video video = videoService.getVideoById(id);

            if (video != null) {
                String resultJson = com.alibaba.fastjson.JSON.toJSONString(Result.success(video));
                resp.getWriter().write(resultJson);
            } else {
                resp.getWriter().write(JSONUtil.toJson(Result.error("视频不存在")));
            }
        } catch (Exception e) {
            log.error("JSON 转换或业务逻辑崩溃: ", e);
            resp.setStatus(500);
            resp.getWriter().write("{\"code\":500, \"msg\":\"服务器内部 JSON 解析错误\"}");
        }
    }

}
