package com.nextstep.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 32)
    @Schema(description = "用户名", example = "alice")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "密码", example = "secret123")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;
}
