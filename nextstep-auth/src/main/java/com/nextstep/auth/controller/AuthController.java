package com.nextstep.auth.controller;

import com.nextstep.auth.dto.LoginRequest;
import com.nextstep.auth.dto.LoginResponse;
import com.nextstep.auth.dto.RegisterRequest;
import com.nextstep.auth.service.AuthService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.LoginUser;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<LoginUser> me() {
        return R.ok(SecurityUtils.current());
    }
}
