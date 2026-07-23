package com.nextstep.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";

    public static final String REDIS_USER_TOKEN = "nextstep:user:token:";
    public static final String REDIS_LOGIN_FAIL = "nextstep:user:login_fail:";
}
