package com.nextstep.admin.controller;

import com.nextstep.admin.dto.AdminStats;
import com.nextstep.admin.service.AdminStatsService;
import com.nextstep.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-系统概览")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "系统资源概览统计")
    @GetMapping("/overview")
    public R<AdminStats> overview() {
        return R.ok(adminStatsService.overview());
    }
}
