package com.nextstep.ai.controller;

import com.nextstep.ai.dto.TranscriptApplyResult;
import com.nextstep.ai.dto.TranscriptExtractResult;
import com.nextstep.ai.service.TranscriptExtractService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "成绩单识别")
@RestController
@RequestMapping("/ai/transcript")
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptExtractService service;

    @Operation(summary = "上传成绩单图片/PDF，返回 LLM 抽取结果（不入库）")
    @PostMapping("/parse")
    public R<TranscriptExtractResult> parse(@RequestPart("file") MultipartFile file) {
        return R.ok(service.parse(file));
    }

    @Operation(summary = "确认抽取结果并入库（课程入库 + 可选同步画像 GPA）")
    @PostMapping("/apply")
    public R<TranscriptApplyResult> apply(
            @Valid @RequestBody TranscriptExtractResult result,
            @RequestParam(name = "syncProfileGpa", defaultValue = "true") boolean syncProfileGpa) {
        return R.ok(service.apply(SecurityUtils.currentUserId(), result, syncProfileGpa));
    }
}
