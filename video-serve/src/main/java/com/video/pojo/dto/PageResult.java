package com.video.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private long total;     // 总记录数
    private List<T> records; // 当前页的数据列表

    public static <T> PageResult<T> build(long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}