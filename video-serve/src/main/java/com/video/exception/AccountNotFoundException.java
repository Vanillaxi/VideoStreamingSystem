package com.video.exception;

import static com.video.constant.MessageConstant.ACCOUNT_NOT_FOUND;

/**
 * 账号不存在异常
 */
public class AccountNotFoundException extends BaseException {

    public AccountNotFoundException() {
        super(401,ACCOUNT_NOT_FOUND );
    }
}
