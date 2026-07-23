package com.nextstep.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT")
    private String token;

    @Schema(description = "过期毫秒")
    private long expireMillis;

    @Schema(description = "用户名")
    private String username;
}
