package com.video.exception;

import lombok.Getter;

@Getter // 方便 BaseController 获取 code
public class BaseException extends RuntimeException {
    private Integer code;

    public BaseException(String msg) {
        super(msg);
        this.code = 500; // 默认错误码
    }

    public BaseException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}