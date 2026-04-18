package com.video.exception;

import static com.video.constant.MessageConstant.PASSWORD_ERROR;

/**
 * 用户名或密码错误错误
 */
public class PasswordErrorException extends BaseException {
    public PasswordErrorException() {
        super(401, PASSWORD_ERROR);
    }
}