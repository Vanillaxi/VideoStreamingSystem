package com.video.exception;

public class BusinessException extends BaseException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(String message) {
        super(message);
    }
}
