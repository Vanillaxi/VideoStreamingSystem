package com.video.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ACCOUNT_ALREADY_EXIST(400, "账号已存在"),
    ACCOUNT_NOT_FOUND(401, "账号不存在"),
    PASSWORD_ERROR(401, "密码错误"),
    USER_NOT_LOGIN(401, "用户未登录"),
    VIDEO_NOT_FOUND(404, "视频不存在"),
    COMMENT_NOT_FOUND(404, "评论不存在"),
    NOT_ALLOW_DELETE(403, "不允许删除别人的评论"),
    NOT_ALLOW_FOLLOW(400, "不允许关注自己"),
    PARENT_COMMENT_NOT_FOUND(400, "父评论不存在或已删除"),
    OSS_DELETE_FAILED(500, "OSS 文件删除失败"),
    LIKE_OPERATION_FAILED(500, "点赞操作失败"),
    FAVORITE_OPERATION_FAILED(500, "收藏失败"),
    CANCEL_FAVORITE_FAILED(500, "取消收藏失败"),
    FOLLOW_OPERATION_FAILED(500, "关注操作失败"),
    VIEW_COUNT_FLUSH_FAILED(500, "播放量刷库失败"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
