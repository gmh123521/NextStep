package com.nextstep.data.school.controller;

import com.nextstep.common.core.PageResult;
import com.nextstep.common.core.R;
import com.nextstep.data.school.entity.School;
import com.nextstep.data.school.entity.SchoolEnroll;
import com.nextstep.data.school.entity.SchoolMajor;
import com.nextstep.data.school.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "考研数据")
@RestController
@RequestMapping("/data/school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @Operation(summary = "院校分页查询")
    @GetMapping
    public R<PageResult<School>> page(
            @RequestParam(name = "pageNum",  defaultValue = "1")  int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword",  required = false) String keyword,
            @RequestParam(name = "level",    required = false) String level,
            @RequestParam(name = "province", required = false) String province) {
        return R.ok(schoolService.page(pageNum, pageSize, keyword, level, province));
    }

    @Operation(summary = "院校招生专业")
    @GetMapping("/{schoolId}/majors")
    public R<List<SchoolMajor>> majors(@PathVariable("schoolId") Long schoolId) {
        return R.ok(schoolService.listMajors(schoolId));
    }

    @Operation(summary = "专业历年招录")
    @GetMapping("/majors/{majorId}/enrolls")
    public R<List<SchoolEnroll>> enrolls(@PathVariable("majorId") Long majorId) {
        return R.ok(schoolService.listEnrolls(majorId));
    }

    @Operation(summary = "专业上岸率统计")
    @GetMapping("/majors/{majorId}/admit-stats")
    public R<List<Map<String,Object>>> admitStats(@PathVariable("majorId") Long majorId) {
        return R.ok(schoolService.admitStats(majorId));
    }
}
