package com.video.basedao.impl;

import com.video.basedao.Dao;
import com.video.basedao.VideoDao;
import com.video.entity.Video;

public class VideoDaoImpl  implements VideoDao {

    /**
     * 根据id查询
     * @param id
     * @return
     */
    @Override
    public Video getById(Long id) {
        String sql = "select id, title, url, user_id as userId, create_time as createTime from videos where id = ?";
        java.util.List<Video>list= Dao.executeQuery(Video.class,sql,id);

        if(list==null||list.isEmpty()){
            return null;
        }
        return list.get(0);
    }

}
