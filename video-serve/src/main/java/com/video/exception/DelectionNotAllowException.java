package com.video.exception;

import static com.video.constant.MessageConstant.NOT_ALLOW_DELETE;

/**
 * 不允许删除
 */
public class DelectionNotAllowException extends BaseException {

    public DelectionNotAllowException() {
        super(401,NOT_ALLOW_DELETE);
    }
}
