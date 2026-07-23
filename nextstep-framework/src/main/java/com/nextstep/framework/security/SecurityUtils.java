package com.nextstep.framework.security;

import com.nextstep.common.core.ResultCode;
import com.nextstep.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser lu)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return lu;
    }

    public static Long currentUserId() {
        return current().userId();
    }
}
