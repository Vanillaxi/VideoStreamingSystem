package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.mapper.CategoryMapper;
import com.video.pojo.entity.Category;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

import java.util.List;

@MyComponent
public class CategoryMapperImpl implements CategoryMapper {
    @Override
    public List<Category> listEnabled() {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CategoryMapper.listEnabled");
        return JdbcUtils.executeQuery(Category.class, sql);
    }

    @Override
    public Category getById(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CategoryMapper.getById");
        List<Category> categories = JdbcUtils.executeQuery(Category.class, sql, id);
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.get(0);
    }
}
