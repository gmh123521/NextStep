package com.nextstep.common.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务返回码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "OK"),
    FAIL(500, "服务异常"),

    PARAM_INVALID(4001, "参数校验失败"),
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    FORBIDDEN(4030, "无权访问"),
    NOT_FOUND(4040, "资源不存在"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "账号或密码错误"),
    USER_ALREADY_EXISTS(10003, "用户已存在"),
    TOKEN_INVALID(10010, "Token 无效"),
    TOKEN_EXPIRED(10011, "Token 已过期");

    private final int code;
    private final String msg;
}
