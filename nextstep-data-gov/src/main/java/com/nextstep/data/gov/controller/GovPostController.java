package com.nextstep.data.gov.controller;

import com.nextstep.common.core.R;
import com.nextstep.data.gov.service.GovPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "考公数据")
@RestController
@RequestMapping("/data/gov")
@RequiredArgsConstructor
public class GovPostController {

    private final GovPostService govPostService;

    @Operation(summary = "岗位搜索")
    @GetMapping("/posts")
    public R<List<Map<String,Object>>> search(
            @RequestParam(name = "year",     required = false) Integer year,
            @RequestParam(name = "examType", required = false) String examType,
            @RequestParam(name = "province", required = false) String province,
            @RequestParam(name = "keyword",  required = false) String keyword) {
        return R.ok(govPostService.search(year, examType, province, keyword));
    }

    @Operation(summary = "岗位详情（含招录、进面线、上岸率）")
    @GetMapping("/posts/{postId}")
    public R<Map<String,Object>> detail(@PathVariable("postId") Long postId) {
        return R.ok(govPostService.detail(postId));
    }
}
