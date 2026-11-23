package com.tva.startrail.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为 Result
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String msg) {
        this(400, msg);
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}
