package com.nextstep.ai.controller;

import com.nextstep.ai.entity.UserExperience;
import com.nextstep.ai.service.ExperienceSummaryService;
import com.nextstep.ai.service.UserExperienceService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户经历")
@RestController
@RequestMapping("/user/experiences")
@RequiredArgsConstructor
public class UserExperienceController {

    private final UserExperienceService experienceService;
    private final ExperienceSummaryService summaryService;

    @Operation(summary = "查询当前用户全部经历")
    @GetMapping
    public R<List<UserExperience>> list() {
        return R.ok(experienceService.list(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "删除一条经历")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        experienceService.delete(SecurityUtils.currentUserId(), id);
        return R.ok();
    }

    @Operation(summary = "回填当前用户经历的 AI 摘要（force=true 强刷全部，否则只补空白）")
    @PostMapping("/backfill-summary")
    public R<Map<String, Object>> backfillSummary(@RequestParam(defaultValue = "false") boolean force) {
        Long userId = SecurityUtils.currentUserId();
        int updated = summaryService.backfillForUser(userId, !force);
        return R.ok(Map.of("updated", updated));
    }
}
