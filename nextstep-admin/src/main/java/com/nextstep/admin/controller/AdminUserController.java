package com.nextstep.admin.controller;

import com.nextstep.admin.service.AdminUserService;
import com.nextstep.auth.entity.User;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户分页")
    @GetMapping
    public R<PageResult<User>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String role) {
        return R.ok(adminUserService.page(pageNum, pageSize, keyword, status, role));
    }

    @Operation(summary = "禁用用户")
    @PutMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        adminUserService.setStatus(id, 1);
        return R.ok();
    }

    @Operation(summary = "启用用户")
    @PutMapping("/{id}/enable")
    public R<Void> enable(@PathVariable Long id) {
        adminUserService.setStatus(id, 0);
        return R.ok();
    }

    @Operation(summary = "设置角色 USER/ADMIN")
    @PutMapping("/{id}/role")
    public R<Void> setRole(@PathVariable Long id, @RequestParam String role) {
        adminUserService.setRole(id, role);
        return R.ok();
    }
}
