package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.exception.VideoNotFoundException;
import com.video.pojo.entity.Video;
import com.video.mapper.VideoMapper;
import com.video.utils.XmlSqlReaderUtil;
import com.video.utils.JdbcUtils;
import java.util.List;

import static com.video.utils.JdbcUtils.executeQuery;
import static com.video.utils.JdbcUtils.executeQueryCount;

@MyComponent
public class VideoMapperImpl implements VideoMapper {

    /**
     * 根据视频id查找
     * @param id
     * @return
     */
    @Override
    public Video getById(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getById");
        List<Video> list = JdbcUtils.executeQuery(Video.class, sql, id);

        if (list == null || list.isEmpty()) {
            throw new VideoNotFoundException();
        }

        return list.get(0);
    }

    /**
     * 发视频
     * @param video
     */
    @Override
    public void postVideo(Video video) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.postVideo");
        JdbcUtils.executeUpdate(sql, video.getTitle(), video.getUrl(), video.getUserId(), video.getLikesCount(), video.getCreateTime());
    }

    /**
     * 点赞
     * @param videoId
     * @param i
     */
    @Override
    public void updateLikes(Long videoId,int i) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.updateLikes");
        JdbcUtils.executeUpdate(sql,i,videoId);
    }

    /**
     * 模糊查询
     * @param title
     * @return
     */
    @Override
    public List<Video> getVideoPageByTitle(String title, int offset, int pageSize) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getVideoPageByTitle");
        return executeQuery(Video.class, sql,title, offset, pageSize);
    }

    /**
     * 模糊查询数量
     * @param title
     * @return
     */
    @Override
    public Long getVideoCountByTitle(String title) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.getVideoCountByTitle");
        return executeQueryCount( sql, title);
    }


    //在video_like 里面操作
    @Override
    public void insertLikes(Long videoId, Long userId) {
        String  sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.insertLikes");
        JdbcUtils.executeUpdate(sql,videoId,userId);
    }

    //在video_like 里面操作
    @Override
    public void deleteLikes(Long videoId, Long userId) {
        String  sql = XmlSqlReaderUtil.getSql("com.video.mapper.VideoMapper.deleteLikes");
        JdbcUtils.executeUpdate(sql,videoId,userId);
    }
}