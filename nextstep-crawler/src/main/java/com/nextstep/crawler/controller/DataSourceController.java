package com.nextstep.crawler.controller;

import com.nextstep.common.core.R;
import com.nextstep.crawler.entity.DataSource;
import com.nextstep.crawler.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-数据源配置")
@RestController
@RequestMapping("/admin/data-import/sources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService sourceService;

    @Operation(summary = "数据源列表")
    @GetMapping
    public R<List<DataSource>> list() {
        return R.ok(sourceService.list());
    }

    @Operation(summary = "更新数据源配置")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestParam(required = false) String sourceUrl,
                          @RequestParam(required = false) Integer enabled,
                          @RequestParam(required = false) String parserVersion) {
        sourceService.update(id, sourceUrl, enabled, parserVersion);
        return R.ok();
    }
}
