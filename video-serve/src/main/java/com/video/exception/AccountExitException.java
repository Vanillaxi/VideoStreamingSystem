package com.video.exception;

import static com.video.constant.MessageConstant.ACCOUNT_ALREADY_EXIST;

public class AccountExitException extends BaseException {
    public AccountExitException() {
        super(400,ACCOUNT_ALREADY_EXIST);
    }
}