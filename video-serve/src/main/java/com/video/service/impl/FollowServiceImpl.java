package com.video.service.impl;


import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.NotAllowFollowException;
import com.video.mapper.FollowMapper;
import com.video.pojo.dto.PageResult;
import com.video.pojo.entity.User;
import com.video.service.FollowService;
import com.video.utils.CacheClient;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@MyComponent
public class FollowServiceImpl implements FollowService {
    @MyAutowired
    private CacheClient cacheClient;

    @MyAutowired
    private FollowMapper followMapper;

    /**
     * 我的关注
     * @param userId
     * @return
     */
    @Override
    public PageResult findFollowings(Long userId, int page, int pageSize) {
        Long total = followMapper.countFollowings(userId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<User> userList = followMapper.findFollowings( userId, offset, pageSize);
        return new PageResult(total, userList);
    }

    /**
     * 我的粉丝
     * @param userId
     * @return
     */
    @Override
    public PageResult findFollowers(Long userId, int page, int pageSize){
        Long total = followMapper.countFollowers(userId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<User> userList = followMapper.findFollowers( userId, offset, pageSize);
        return new PageResult(total, userList);
    }

    /**
     * 我的互粉
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageResult findFriends(Long userId, int page, int pageSize) {
        Long total = followMapper.countFriends(userId);

        if (total == null || total == 0) {
            return new PageResult(0L, new ArrayList<>());
        }

        int offset = (page - 1) * pageSize;
        List<User> userList = followMapper.findFriends( userId, offset, pageSize);
        return new PageResult(total, userList);
    }


    /**
     * 使用ZSet实现更改关注
     * @param targetUserId
     */
    @Override
    public void changeFollow(Long targetUserId) {
        Long myId = UserHolder.getUser().getId();
        if(myId.equals(targetUserId)){
            throw new NotAllowFollowException();
        }
        String followingKey = "user:following:" + myId;
        String followerKey = "user:followers:" + targetUserId;

        Double score = RedisUtil.zscore(followingKey, targetUserId.toString());

        if (score == null) {
            Long rows = followMapper.follow( targetUserId,myId);
            if(rows>0){
                double now = (double) System.currentTimeMillis();
                RedisUtil.zadd(followingKey, now, targetUserId.toString());
                RedisUtil.zadd(followerKey, now, myId.toString());
                log.info("用户 {} 关注了 {}", myId, targetUserId);
            }
        } else {
            Long rows = followMapper.unFollow(targetUserId,myId);
            if(rows>0){
                RedisUtil.zrem(followingKey, targetUserId.toString());
                RedisUtil.zrem(followerKey, myId.toString());
                log.info("用户 {} 取消关注了 {}", myId, targetUserId);
            }
        }
    }

    /**
     * 查看是否关注
     */
    @Override
    public Boolean isFollow(Long idA) {
        Long myId = UserHolder.getUser().getId();
        Double IFollowsA = RedisUtil.zscore("user:followers:" + idA, myId.toString());
        return IFollowsA != null;
    }

}
