package com.nextstep.user.controller;

import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户画像")
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "获取当前用户画像")
    @GetMapping
    public R<UserProfile> get() {
        return R.ok(userProfileService.get(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "创建/更新当前用户画像")
    @PutMapping
    public R<UserProfile> upsert(@Valid @RequestBody UserProfileRequest req) {
        return R.ok(userProfileService.upsert(SecurityUtils.currentUserId(), req));
    }
}
