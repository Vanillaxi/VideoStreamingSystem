package com.video.exception;

import static com.video.constant.MessageConstant.VIDEO_NOT_FOUND;

/**
 * 视频不存在异常
 */
public class VideoNotFoundException extends BaseException {

    public VideoNotFoundException() {
        super(401,VIDEO_NOT_FOUND );
    }
}
