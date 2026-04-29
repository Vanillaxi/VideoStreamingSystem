package com.video.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeedPageResult<T> {
    private List<T> list;
    private Double nextLastScore;
    private Long nextLastId;
    private Boolean hasMore;
    private Integer pageSize;

    public FeedPageResult(List<T> list, Double nextLastScore, Long nextLastId, Boolean hasMore, Integer pageSize) {
        this.list = list;
        this.nextLastScore = nextLastScore;
        this.nextLastId = nextLastId;
        this.hasMore = hasMore;
        this.pageSize = pageSize;
    }
}
