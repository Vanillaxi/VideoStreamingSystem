package com.video.service;

import com.video.pojo.dto.PageResult;

public interface FollowService {
    //查看关注
    PageResult findFollowings(Long userId, int page, int pageSize);

    //查看粉丝
    PageResult findFollowers(Long userId, int page, int pageSize);

    //查看互关
    PageResult findFriends(Long userId, int page, int pageSize);


    //更改关注
    String changeFollow(Long followingId);


    Boolean isFollow(Long idA);
}
