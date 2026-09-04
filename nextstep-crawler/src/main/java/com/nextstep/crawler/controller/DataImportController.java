package com.nextstep.crawler.controller;

import com.nextstep.common.core.PageResult;
import com.nextstep.common.core.R;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.entity.DataRawRecord;
import com.nextstep.crawler.service.DataImportBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 管理员数据导入批次审核与发布。 */
@Tag(name = "后台-数据批次")
@RestController
@RequestMapping("/admin/data-import/batches")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportBatchService batchService;

    @Operation(summary = "导入批次分页")
    @GetMapping
    public R<PageResult<DataImportBatch>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer dataYear) {
        return R.ok(batchService.page(pageNum, pageSize, sourceCode, status, dataYear));
    }

    @Operation(summary = "导入批次详情")
    @GetMapping("/{id}")
    public R<DataImportBatch> detail(@PathVariable Long id) {
        return R.ok(batchService.detail(id));
    }

    @Operation(summary = "批次解析错误分页")
    @GetMapping("/{id}/errors")
    public R<PageResult<DataRawRecord>> errors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(batchService.errors(id, pageNum, pageSize));
    }

    @Operation(summary = "审核通过批次")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        batchService.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回批次")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        batchService.reject(id, reason);
        return R.ok();
    }

    @Operation(summary = "发布批次")
    @PutMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        batchService.publish(id);
        return R.ok();
    }

    @Operation(summary = "回滚批次")
    @PutMapping("/{id}/rollback")
    public R<Void> rollback(@PathVariable Long id, @RequestParam(required = false) String reason) {
        batchService.rollback(id, reason);
        return R.ok();
    }

    @Operation(summary = "重新解析原始快照")
    @PutMapping("/{id}/reparse")
    public R<Void> reparse(@PathVariable Long id) {
        batchService.reparse(id);
        return R.ok();
    }
}
