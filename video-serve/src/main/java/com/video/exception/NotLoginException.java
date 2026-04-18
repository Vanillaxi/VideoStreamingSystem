package com.video.exception;

import static com.video.constant.MessageConstant.USER_NOT_LOGIN;

/**
 * 未登录
 */
public class NotLoginException extends BaseException {

    public NotLoginException() {
        super(401, USER_NOT_LOGIN);
    }
}
