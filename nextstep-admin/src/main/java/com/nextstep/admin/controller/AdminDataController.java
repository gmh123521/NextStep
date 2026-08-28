package com.nextstep.admin.controller;

import com.nextstep.admin.service.AdminDataService;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.core.R;
import com.nextstep.data.gov.entity.GovPost;
import com.nextstep.data.job.entity.JobPosition;
import com.nextstep.data.job.entity.SalaryStat;
import com.nextstep.data.school.entity.School;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台-数据维护")
@RestController
@RequestMapping("/admin/data")
@RequiredArgsConstructor
public class AdminDataController {

    private final AdminDataService adminDataService;

    // ── 院校 ──────────────────────────────────────────────────────────────────

    @Operation(summary = "院校分页")
    @GetMapping("/schools")
    public R<PageResult<School>> pageSchool(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String province) {
        return R.ok(adminDataService.pageSchool(pageNum, pageSize, keyword, level, province));
    }

    @Operation(summary = "新增/更新院校")
    @PostMapping("/schools")
    public R<Long> saveSchool(@RequestBody School school) {
        return R.ok(adminDataService.saveSchool(school));
    }

    @Operation(summary = "删除院校")
    @DeleteMapping("/schools/{id}")
    public R<Void> deleteSchool(@PathVariable Long id) {
        adminDataService.deleteSchool(id);
        return R.ok();
    }

    // ── 考公岗位 ──────────────────────────────────────────────────────────────

    @Operation(summary = "考公岗位分页")
    @GetMapping("/gov-posts")
    public R<PageResult<GovPost>> pageGovPost(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminDataService.pageGovPost(pageNum, pageSize, year, province, keyword));
    }

    @Operation(summary = "新增/更新考公岗位")
    @PostMapping("/gov-posts")
    public R<Long> saveGovPost(@RequestBody GovPost post) {
        return R.ok(adminDataService.saveGovPost(post));
    }

    @Operation(summary = "删除考公岗位")
    @DeleteMapping("/gov-posts/{id}")
    public R<Void> deleteGovPost(@PathVariable Long id) {
        adminDataService.deleteGovPost(id);
        return R.ok();
    }

    // ── 就业岗位 ──────────────────────────────────────────────────────────────

    @Operation(summary = "就业岗位分页")
    @GetMapping("/job-positions")
    public R<PageResult<JobPosition>> pageJobPosition(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return R.ok(adminDataService.pageJobPosition(pageNum, pageSize, keyword, category));
    }

    @Operation(summary = "新增/更新就业岗位")
    @PostMapping("/job-positions")
    public R<Long> saveJobPosition(@RequestBody JobPosition position) {
        return R.ok(adminDataService.saveJobPosition(position));
    }

    @Operation(summary = "删除就业岗位")
    @DeleteMapping("/job-positions/{id}")
    public R<Void> deleteJobPosition(@PathVariable Long id) {
        adminDataService.deleteJobPosition(id);
        return R.ok();
    }

    // ── 薪资统计 ──────────────────────────────────────────────────────────────

    @Operation(summary = "薪资统计分页")
    @GetMapping("/salary-stats")
    public R<PageResult<SalaryStat>> pageSalaryStat(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer statYear) {
        return R.ok(adminDataService.pageSalaryStat(pageNum, pageSize, positionId, city, statYear));
    }

    @Operation(summary = "新增/更新薪资统计")
    @PostMapping("/salary-stats")
    public R<Long> saveSalaryStat(@RequestBody SalaryStat stat) {
        return R.ok(adminDataService.saveSalaryStat(stat));
    }

    @Operation(summary = "删除薪资统计")
    @DeleteMapping("/salary-stats/{id}")
    public R<Void> deleteSalaryStat(@PathVariable Long id) {
        adminDataService.deleteSalaryStat(id);
        return R.ok();
    }
}
