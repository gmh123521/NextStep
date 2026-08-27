package com.nextstep.crawler.controller;

import com.nextstep.common.core.PageResult;
import com.nextstep.common.core.R;
import com.nextstep.crawler.entity.CrawlerJob;
import com.nextstep.crawler.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台采集管理：路径前缀 /admin/crawler，受 SecurityConfig 的 hasRole(ADMIN) 保护。
 */
@Tag(name = "后台-采集管理")
@RestController
@RequestMapping("/admin/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(summary = "可用数据源列表")
    @GetMapping("/sources")
    public R<List<String>> sources() {
        return R.ok(crawlerService.sources());
    }

    @Operation(summary = "手动触发指定数据源采集")
    @PostMapping("/run/{source}")
    public R<CrawlerJob> run(@PathVariable String source) {
        return R.ok(crawlerService.run(source, "MANUAL"));
    }

    @Operation(summary = "采集任务运行记录分页")
    @GetMapping("/jobs")
    public R<PageResult<CrawlerJob>> jobs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String source) {
        return R.ok(crawlerService.pageJobs(pageNum, pageSize, source));
    }
}
