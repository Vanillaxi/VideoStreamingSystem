package com.video.mapper;

import com.video.pojo.entity.Category;

import java.util.List;

public interface CategoryMapper {
    List<Category> listEnabled();

    Category getById(Long id);
}
