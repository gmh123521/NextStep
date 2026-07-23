package com.nextstep.analysis.controller;

import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.service.AnalysisService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评分分析")
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "三路径评分 + 推荐")
    @GetMapping("/score")
    public R<AnalysisResult> score() {
        return R.ok(analysisService.analyze(SecurityUtils.currentUserId()));
    }
}
