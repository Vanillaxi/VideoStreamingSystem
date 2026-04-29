package com.video.pojo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CursorPageResult<T> {
    private List<T> records;
    private Boolean hasNext;
    private Double nextHotScore;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextCreateTime;

    private Long nextId;
    private Integer pageSize;

    public CursorPageResult(List<T> records, Boolean hasNext, Double nextHotScore,
                            LocalDateTime nextCreateTime, Long nextId, Integer pageSize) {
        this.records = records;
        this.hasNext = hasNext;
        this.nextHotScore = nextHotScore;
        this.nextCreateTime = nextCreateTime;
        this.nextId = nextId;
        this.pageSize = pageSize;
    }
}
