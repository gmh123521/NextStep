package com.nextstep.report.controller;

import com.nextstep.framework.security.SecurityUtils;
import com.nextstep.report.service.ReportPdfService;
import com.nextstep.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Tag(name = "综合报告")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;

    @Operation(summary = "导出综合决策报告 PDF")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        Long userId = SecurityUtils.currentUserId();
        String username = SecurityUtils.current().username();

        byte[] pdf = reportPdfService.render(reportService.assemble(userId, username));

        String filename = "NextStep报告_" + LocalDate.now() + ".pdf";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .body(pdf);
    }
}
