package com.video.service.impl;

import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.mapper.CategoryMapper;
import com.video.pojo.entity.Category;
import com.video.service.CategoryService;

import java.util.List;

@MyComponent
public class CategoryServiceImpl implements CategoryService {
    @MyAutowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listCategories() {
        return categoryMapper.listEnabled();
    }
}
