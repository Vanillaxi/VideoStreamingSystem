package com.video.test;


import com.video.proxy.BeanFactory;
import com.video.mapper.VideoMapper;
import com.video.pojo.entity.Video;
import com.video.utils.CacheClient;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;


public class CachePreheatTest {

    @Test
    public void preheatVideoData() {
        BeanFactory.init();
        CacheClient cacheClient = BeanFactory.getBean(CacheClient.class);
        VideoMapper videoMapper = BeanFactory.getBean(VideoMapper.class);

        // 确定热点数据的 ID
        Long[] hotVideoIds = {2L,3L,4L};

        for (Long id : hotVideoIds) {
            Video video = videoMapper.getById(id);
            if (video != null) {
                cacheClient.setWithLogicalExpire("video:" + id, video, 30L, TimeUnit.MINUTES);
                System.out.println("预热视频成功，ID: " + id);
            }
        }
    }
}