package com.video.mapper;

import com.video.pojo.entity.VideoFavorite;

import java.util.List;

public interface FavoriteMapper {
    int insertFavorite(Long videoId, Long userId);
    int insertFavoriteWithTransaction(Long videoId, Long userId);

    int deleteFavorite(Long videoId, Long userId);
    int deleteFavoriteWithTransaction(Long videoId, Long userId);

    boolean existsFavorite(Long videoId, Long userId);

    List<VideoFavorite> findByUserId(Long userId);

    Long countByUserId(Long userId);
}
