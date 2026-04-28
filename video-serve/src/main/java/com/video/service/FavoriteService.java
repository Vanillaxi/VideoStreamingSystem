package com.video.service;

import com.video.pojo.dto.PageResult;

public interface FavoriteService {
    void favorite(Long videoId);

    void cancelFavorite(Long videoId);

    Boolean isFavorite(Long videoId);

    PageResult getFavoriteList(int page, int pageSize);
}
