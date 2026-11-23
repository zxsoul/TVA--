package com.tva.startrail.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应对象（技术文档 §2.1）
 * code：0=成功；4xx/5xx HTTP 语义；10xxx 业务码（本期预留）
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok(String msg, T data) {
        Result<T> r = ok(data);
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
