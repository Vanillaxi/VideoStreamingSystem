package com.video.service.impl;

import com.video.annotation.MyComponent;
import com.video.basedao.VideoDao;
import com.video.basedao.impl.VideoDaoImpl;
import com.video.entity.Video;
import com.video.service.VideoService;
import com.video.utils.JSONUtil;
import com.video.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MyComponent
public class VideoServiceImpl implements VideoService {

    private VideoDao videoDao=new VideoDaoImpl();

    @Override
    public Video getVideoById(Long id){
        String key="video:detail:"+id;

        //先查Redis
        String json = RedisUtil.get(key);
        if(json!=null){
            log.info("【Redis命中】视频ID:{}",id);
            return JSONUtil.toBean(json,Video.class);
        }

        //查数据库
        log.info("【数据库查询】视频ID:{}",id);
        Video video =videoDao.getById(id);

        return video;
    }


}
