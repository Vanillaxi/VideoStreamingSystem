package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.VideoNotFoundException;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.User;
import com.video.pojo.entity.Video;
import com.video.mapper.VideoMapper;
import com.video.pojo.dto.Result;
import com.video.service.VideoService;
import com.video.utils.CacheClient;
import com.video.utils.JdbcUtils;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@MyComponent
public class VideoServiceImpl implements VideoService {

    @MyAutowired
    private VideoMapper videoMapper;

    @MyAutowired
    private CacheClient cacheClient;

    /**
     * 根据视频id查视频
     * @param id
     * @return
     */
    @Override
    public Video getVideoById(Long id){
        String prefix="video:";
        Video video= cacheClient
                .queryWithLogicalExpire(prefix,id,Video.class,this::getByIdFromDb,30L, TimeUnit.MINUTES);

        if(video==null){
            System.out.println("ERRROR:IMPL");
            throw new VideoNotFoundException();
        }

        System.out.println("DEBUG: 数据库查询结果对象 = " + video);

        return video;
    }

    /**
     * 发布视频
     * @param video
     */
    @Override
    public void postVideo(Video video) {
        video.setUserId(UserHolder.getUser().getId());
        video.setCreateTime(LocalDateTime.now());
        videoMapper.postVideo(video);
    }

    /**
     * 更改点赞视频 (ZSet)
     * 还需要再video_like里面操作,以后可以实现好友都点赞的，然后进行推荐
     * @param videoId
     */
    @Override
    public void changeLikeVideo(Long videoId) {
        Long userId = UserHolder.getUser().getId();
        String key = "video:liked:" + videoId;

        Double score = RedisUtil.zscore(key, userId.toString());

        if (score == null) {
            videoMapper.insertLikes(videoId, userId);
            videoMapper.updateLikes(videoId,1);
            RedisUtil.zadd(key, (double) System.currentTimeMillis(), userId.toString());
        } else {
            videoMapper.deleteLikes(videoId, userId);
            videoMapper.updateLikes(videoId,-1);
            RedisUtil.zrem(key, userId.toString());
        }
    }

    /**
     * 根据title模糊查询视频
     * @param title
     * @return
     */
    @Override
    public PageResult getVideoTitle(String title, int page, int pageSize) {
        System.out.println("IMPLLLLLL");
        String searchTitle = (title == null || title.isEmpty()) ? "%%" : "%" + title + "%";

        Long total = videoMapper.getVideoCountByTitle(searchTitle);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        System.out.println("IMPLAFTER");
        int offset = (page - 1) * pageSize;
        List<Video> videoList = videoMapper.getVideoPageByTitle(searchTitle, offset, pageSize);

        return new PageResult(total, videoList);
    }

    //从db查视频
    private Video getByIdFromDb(Long id) {
        return videoMapper.getById(id);
    }


}
