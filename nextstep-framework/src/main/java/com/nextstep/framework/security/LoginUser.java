package com.nextstep.framework.security;

import java.io.Serial;
import java.io.Serializable;

public record LoginUser(Long userId, String username) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
