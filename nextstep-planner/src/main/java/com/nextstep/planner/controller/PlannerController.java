package com.nextstep.planner.controller;

import com.nextstep.common.core.R;
import com.nextstep.common.exception.BizException;
import com.nextstep.framework.security.SecurityUtils;
import com.nextstep.planner.dto.PlanView;
import com.nextstep.planner.service.PdfExportService;
import com.nextstep.planner.service.PlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "备考/求职规划")
@RestController
@RequestMapping("/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;
    private final PdfExportService pdfExportService;

    @Operation(summary = "查询当前路径的规划")
    @GetMapping
    public R<PlanView> get(@RequestParam("path") String path) {
        return R.ok(plannerService.get(SecurityUtils.currentUserId(), path));
    }

    @Operation(summary = "推荐战线长度（基于年级/状态智能推断）")
    @GetMapping("/recommend")
    public R<java.util.Map<String, Object>> recommend(@RequestParam("path") String path) {
        return R.ok(plannerService.recommendMonths(SecurityUtils.currentUserId(), path));
    }

    @Operation(summary = "生成（或重新生成）某路径规划，可选 months 自定义战线（3-24）")
    @PostMapping("/generate")
    public R<PlanView> generate(@RequestParam("path") String path,
                                @RequestParam(name = "months", required = false) Integer months) {
        return R.ok(plannerService.generate(SecurityUtils.currentUserId(), path, months));
    }

    @Operation(summary = "勾选/取消勾选任务")
    @PutMapping("/tasks/{taskId}")
    public R<Void> toggleTask(@PathVariable("taskId") Long taskId,
                              @RequestBody ToggleReq req) {
        plannerService.toggleTask(SecurityUtils.currentUserId(), taskId, req.completed);
        return R.ok();
    }

    @Operation(summary = "导出当前路径规划为 PDF")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPdf(@RequestParam("path") String path) {
        Long userId = SecurityUtils.currentUserId();
        PlanView plan = plannerService.get(userId, path);
        if (plan == null) throw new BizException("当前路径还没有规划，无法导出");

        byte[] pdf = pdfExportService.render(plan);
        String filename = "NextStep-" + plan.getPathName() + "规划.pdf";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        // 兼容：filename 给老浏览器，filename* 给现代浏览器（防中文乱码）
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, 200);
    }

    @Data
    public static class ToggleReq {
        private boolean completed;
    }
}
