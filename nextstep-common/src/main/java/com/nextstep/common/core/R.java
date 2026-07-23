package com.nextstep.common.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return build(ResultCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> R<T> fail() {
        return build(ResultCode.FAIL.getCode(), ResultCode.FAIL.getMsg(), null);
    }

    public static <T> R<T> fail(String msg) {
        return build(ResultCode.FAIL.getCode(), msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> R<T> fail(ResultCode rc) {
        return build(rc.getCode(), rc.getMsg(), null);
    }

    private static <T> R<T> build(int code, String msg, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
