package com.video.exception;


import static com.video.constant.MessageConstant.NOT_ALLOW_FOLLOW;

/**
 * 视频不存在异常
 */
public class NotAllowFollowException extends BaseException {

    public NotAllowFollowException() {
        super(401,NOT_ALLOW_FOLLOW );
    }
}
