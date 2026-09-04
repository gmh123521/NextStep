package com.nextstep.data.job.controller;

import com.nextstep.common.core.R;
import com.nextstep.data.job.entity.Industry;
import com.nextstep.data.job.entity.JobPosition;
import com.nextstep.data.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "就业数据")
@RestController
@RequestMapping("/data/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "行业列表")
    @GetMapping("/industries")
    public R<List<Industry>> industries() {
        return R.ok(jobService.listIndustries());
    }

    @Operation(summary = "岗位查询")
    @GetMapping("/positions")
    public R<List<JobPosition>> positions(
            @RequestParam(name = "industryId", required = false) Long industryId,
            @RequestParam(name = "keyword",    required = false) String keyword) {
        return R.ok(jobService.listPositions(industryId, keyword));
    }

    @Operation(summary = "岗位薪资统计")
    @GetMapping("/positions/{positionId}/salary")
    public R<List<Map<String,Object>>> salary(@PathVariable("positionId") Long positionId) {
        return R.ok(jobService.salaryByPosition(positionId));
    }
}
