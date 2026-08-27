package com.nextstep.framework.security;

import java.io.Serial;
import java.io.Serializable;

public record LoginUser(Long userId, String username, String role) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 兼容旧调用：无角色时默认普通用户 */
    public LoginUser(Long userId, String username) {
        this(userId, username, "USER");
    }
}
