package com.video.basedao;

import com.video.entity.Video;

public interface VideoDao {

    /**
     * 根据id查询
     */
    Video getById(Long id);

}
