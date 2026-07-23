package com.nextstep.ai.controller;

import com.nextstep.ai.dto.ResumeApplyResult;
import com.nextstep.ai.dto.ResumeExtractResult;
import com.nextstep.ai.service.ResumeExtractService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "简历解析")
@RestController
@RequestMapping("/ai/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeExtractService resumeExtractService;

    @Operation(summary = "上传简历 PDF，返回 LLM 抽取结果（不入库）")
    @PostMapping("/parse")
    public R<ResumeExtractResult> parse(@RequestPart("file") MultipartFile file) {
        return R.ok(resumeExtractService.parse(file));
    }

    @Operation(summary = "确认抽取结果并入库（合并画像 + 追加经历，自动去重）")
    @PostMapping("/apply")
    public R<ResumeApplyResult> apply(@Valid @RequestBody ResumeExtractResult result) {
        return R.ok(resumeExtractService.apply(SecurityUtils.currentUserId(), result));
    }
}
