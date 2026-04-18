package com.video.controller;


import com.video.annotation.MyAutowired;
import com.video.annotation.MyMapping;
import com.video.pojo.dto.PageResult;
import com.video.pojo.dto.Result;
import com.video.service.FollowService;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/follow/*")
public class FollowController extends BaseController {
    @MyAutowired
    private FollowService followService ;

    /**
     * 我的关注
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @MyMapping(value="/followings",method="GET")
    public Result findFollowings(Long userId, int page, int pageSize){
        PageResult pageResult = followService.findFollowings(userId,page,pageSize);
        return Result.success(pageResult);
    }

    /**
     * 我的粉丝
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @MyMapping(value="/followers",method="GET")
    public Result findFollowers(Long userId, int page, int pageSize){
        PageResult pageResult = followService.findFollowers(userId,page,pageSize);
        return Result.success(pageResult);
    }

    /**
     * 我的互粉
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @MyMapping(value="/friends",method="GET")
    public Result findFriends(Long userId, int page, int pageSize){
        PageResult pageResult = followService.findFriends(userId,page,pageSize);
        return Result.success(pageResult);
    }

    /**
     * 是否关注
     * @param followingId
     * @return
     */
    @MyMapping(value="/isFollow",method="GET")
    public Result isFollow(Long followingId){
        Boolean result = followService.isFollow(followingId);
        return Result.success(result);
    }


    /**
     * 更改关注
     * @param FollowingId
     * @return
     */
    @MyMapping (value="/changeFollow",method="POST")
    public Result changeFollow(Long FollowingId){
        followService.changeFollow(FollowingId);
        return Result.success("修改关注成功");
    }

}
