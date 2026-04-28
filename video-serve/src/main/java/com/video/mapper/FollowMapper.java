package com.video.mapper;


import com.video.pojo.entity.User;
import com.video.pojo.entity.UserFollow;
import java.util.List;

public interface FollowMapper {
    //查询我的粉丝列表
    List<User> findFollowers(Long userId,int offset,int pageSize);
    //查询我的关注列表
    List<User> findFollowings(Long userId,int offset,int pageSize);
    //查询我的互粉列表
    List<User> findFriends (Long userId,int offset,int pageSize);

    //查看是否关注
    Boolean isFollow(Long followingId,Long followerId);

    Long follow(Long followingId, Long followerId);
    Long unFollow(Long followingId, Long followerId);
    int changeFollowWithTransaction(Long followingId, Long followerId);

    Long countFollowings(Long userId);
    Long countFollowers(Long userId);
    Long countFriends(Long userId);

    List<UserFollow> findFollowingRelations(Long userId);
    List<UserFollow> findFollowerRelations(Long userId);
    List<UserFollow> findFriendRelations(Long userId);
}
