package com.video.service.impl;


import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import com.video.exception.BusinessException;
import com.video.exception.ErrorCode;
import com.video.mapper.FollowMapper;
import com.video.mapper.UserMapper;
import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.UserInfoVO;
import com.video.pojo.entity.User;
import com.video.pojo.entity.UserFollow;
import com.video.service.FollowService;
import com.video.utils.CacheClient;
import com.video.utils.RedisUtil;
import com.video.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@MyComponent
public class FollowServiceImpl implements FollowService {
    private static final String FOLLOWING_PREFIX = "user:following:";
    private static final String FOLLOWER_PREFIX = "user:followers:";
    private static final String FRIEND_PREFIX = "user:friends:";

    @MyAutowired
    private CacheClient cacheClient;

    @MyAutowired
    private FollowMapper followMapper;

    @MyAutowired
    private UserMapper userMapper;

    /**
     * 我的关注
     * @param userId
     * @return
     */
    @Override
    public PageResult findFollowings(Long userId, int page, int pageSize) {
        String key = followingKey(userId);
        if (!RedisUtil.exists(key)) {
            rebuildFollowingRedis(userId);
        }
        return getUsersFromZSet(key, page, pageSize);
    }

    /**
     * 我的粉丝
     * @param userId
     * @return
     */
    @Override
    public PageResult findFollowers(Long userId, int page, int pageSize){
        String key = followerKey(userId);
        if (!RedisUtil.exists(key)) {
            rebuildFollowerRedis(userId);
        }
        return getUsersFromZSet(key, page, pageSize);
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
        String key = friendKey(userId);
        if (!RedisUtil.exists(key)) {
            rebuildFriendRedis(userId);
        }
        return getUsersFromZSet(key, page, pageSize);
    }


    /**
     * 使用ZSet实现更改关注
     * @param targetUserId
     */
    @Override
    public String changeFollow(Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException(400, "关注用户ID不能为空");
        }
        Long myId = UserHolder.getUser().getId();
        if(myId.equals(targetUserId)){
            throw new BusinessException(ErrorCode.NOT_ALLOW_FOLLOW);
        }
        int delta = followMapper.changeFollowWithTransaction(targetUserId, myId);
        if (delta != 0) {
            try {
                rebuildFollowingRedis(myId);
                rebuildFollowerRedis(targetUserId);
                rebuildFriendRedis(myId);
                rebuildFriendRedis(targetUserId);
            } catch (Exception e) {
                log.warn("关注 Redis 重建失败，myId={}, targetUserId={}", myId, targetUserId, e);
            }
            if (delta > 0) {
                log.info("用户 {} 关注了 {}", myId, targetUserId);
            } else {
                log.info("用户 {} 取消关注了 {}", myId, targetUserId);
            }
        }
        return delta > 0 ? "关注成功" : "取消关注成功";
    }

    /**
     * 查看是否关注
     */
    @Override
    public Boolean isFollow(Long idA) {
        Long myId = UserHolder.getUser().getId();
        String key = followingKey(myId);
        if (!RedisUtil.exists(key)) {
            rebuildFollowingRedis(myId);
        }
        Double score = RedisUtil.zscore(key, idA.toString());
        return score != null;
    }

    private PageResult getUsersFromZSet(String key, int page, int pageSize) {
        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1;
        List<String> userIdSet = RedisUtil.zrevrange(key, start, end);
        Long total = RedisUtil.zcard(key);
        if (userIdSet == null || userIdSet.isEmpty()) {
            return new PageResult(total == null ? 0L : total, new ArrayList<>());
        }

        List<Long> userIds = userIdSet.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userMapper.getByIds(userIds);
        Map<Long, User> userMap = new LinkedHashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        List<UserInfoVO> orderedUsers = new ArrayList<>();
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user != null) {
                orderedUsers.add(toUserInfoVO(user));
            }
        }
        return new PageResult(total == null ? 0L : total, orderedUsers);
    }

    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setTargetUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setAvatar(user.getAvatarUrl());
        vo.setCreateTime(user.getCreateTime());
        vo.setFollowCount(safeCount(followMapper.countFollowings(user.getId())));
        vo.setFanCount(safeCount(followMapper.countFollowers(user.getId())));
        vo.setMutualFollowCount(safeCount(followMapper.countFriends(user.getId())));
        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().equals(user.getId())) {
            vo.setIsFollowed(false);
        } else {
            vo.setIsFollowed(Boolean.TRUE.equals(followMapper.isFollow(user.getId(), currentUser.getId())));
        }
        return vo;
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    //重建缓存
    private void rebuildFollowingRedis(Long userId) {
        RedisUtil.del(followingKey(userId));
        List<UserFollow> relations = followMapper.findFollowingRelations(userId);
        String key = followingKey(userId);
        int index = 0;
        for (UserFollow relation : relations) {
            RedisUtil.zadd(key, followScore(relation, index), relation.getFollowingId().toString());
            index++;
        }
    }

    //重建缓存
    private void rebuildFollowerRedis(Long userId) {
        RedisUtil.del(followerKey(userId));
        List<UserFollow> relations = followMapper.findFollowerRelations(userId);
        String key = followerKey(userId);
        int index = 0;
        for (UserFollow relation : relations) {
            RedisUtil.zadd(key, followScore(relation, index), relation.getFollowerId().toString());
            index++;
        }
    }

    //重建缓存
    private void rebuildFriendRedis(Long userId) {
        RedisUtil.del(friendKey(userId));
        List<UserFollow> relations = followMapper.findFriendRelations(userId);
        String key = friendKey(userId);
        int index = 0;
        for (UserFollow relation : relations) {
            RedisUtil.zadd(key, followScore(relation, index), relation.getFollowingId().toString());
            index++;
        }
    }

    private double followScore(UserFollow relation, int index) {
        if (relation.getCreateTime() != null) {
            return relation.getCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }
        return System.currentTimeMillis() - index;
    }

    private String followingKey(Long userId) {
        return FOLLOWING_PREFIX + userId;
    }

    private String followerKey(Long userId) {
        return FOLLOWER_PREFIX + userId;
    }

    private String friendKey(Long userId) {
        return FRIEND_PREFIX + userId;
    }

}
