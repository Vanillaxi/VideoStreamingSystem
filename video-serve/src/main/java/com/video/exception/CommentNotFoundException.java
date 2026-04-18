package com.video.exception;

import static com.video.constant.MessageConstant.COMMENT_NOT_FOUND;

/**
 * 评论不存在异常
 */
public class CommentNotFoundException extends BaseException {

    public CommentNotFoundException() {
        super(401,COMMENT_NOT_FOUND );
    }
}
