package com.nextstep.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.exception.BizException;
import com.nextstep.data.gov.entity.GovPost;
import com.nextstep.data.gov.mapper.GovPostMapper;
import com.nextstep.data.job.entity.JobPosition;
import com.nextstep.data.job.entity.SalaryStat;
import com.nextstep.data.job.mapper.JobPositionMapper;
import com.nextstep.data.job.mapper.SalaryStatMapper;
import com.nextstep.data.school.entity.School;
import com.nextstep.data.school.mapper.SchoolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 后台数据维护：院校、考公岗位、就业岗位、薪资统计的增删改查。
 * 前台 data-* 模块只提供只读查询，写入统一收敛到后台，权限受 /admin/** 保护。
 */
@Service
@RequiredArgsConstructor
public class AdminDataService {

    private final SchoolMapper schoolMapper;
    private final GovPostMapper govPostMapper;
    private final JobPositionMapper jobPositionMapper;
    private final SalaryStatMapper salaryStatMapper;

    // ── 院校 ──────────────────────────────────────────────────────────────────

    public PageResult<School> pageSchool(int pageNum, int pageSize, String keyword, String level, String province) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<School>()
                .like(StringUtils.hasText(keyword), School::getName, keyword)
                .eq(StringUtils.hasText(level), School::getLevel, level)
                .eq(StringUtils.hasText(province), School::getProvince, province)
                .orderByAsc(School::getId);
        Page<School> p = schoolMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    public Long saveSchool(School s) {
        if (s == null || !StringUtils.hasText(s.getName())) throw new BizException("院校名称不能为空");
        if (s.getId() == null) {
            schoolMapper.insert(s);
        } else {
            schoolMapper.updateById(s);
        }
        return s.getId();
    }

    public void deleteSchool(Long id) {
        if (schoolMapper.deleteById(id) == 0) throw new BizException("院校不存在：" + id);
    }

    // ── 考公岗位 ──────────────────────────────────────────────────────────────

    public PageResult<GovPost> pageGovPost(int pageNum, int pageSize, Integer year, String province, String keyword) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<GovPost> w = new LambdaQueryWrapper<GovPost>()
                .eq(year != null, GovPost::getYear, year)
                .eq(StringUtils.hasText(province), GovPost::getProvince, province)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(GovPost::getPostName, keyword)
                        .or().like(GovPost::getDeptName, keyword))
                .orderByDesc(GovPost::getYear).orderByAsc(GovPost::getId);
        Page<GovPost> p = govPostMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    public Long saveGovPost(GovPost g) {
        if (g == null || g.getYear() == null || !StringUtils.hasText(g.getDeptName()) || !StringUtils.hasText(g.getPostName())) {
            throw new BizException("考公岗位年份、部门和岗位名称不能为空");
        }
        if (g.getId() == null) {
            govPostMapper.insert(g);
        } else {
            govPostMapper.updateById(g);
        }
        return g.getId();
    }

    public void deleteGovPost(Long id) {
        if (govPostMapper.deleteById(id) == 0) throw new BizException("岗位不存在：" + id);
    }

    // ── 就业岗位 ──────────────────────────────────────────────────────────────

    public PageResult<JobPosition> pageJobPosition(int pageNum, int pageSize, String keyword, String category) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<JobPosition> w = new LambdaQueryWrapper<JobPosition>()
                .like(StringUtils.hasText(keyword), JobPosition::getName, keyword)
                .eq(StringUtils.hasText(category), JobPosition::getCategory, category)
                .orderByAsc(JobPosition::getId);
        Page<JobPosition> p = jobPositionMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    public Long saveJobPosition(JobPosition j) {
        if (j == null || !StringUtils.hasText(j.getName())) throw new BizException("就业岗位名称不能为空");
        if (j.getId() == null) {
            jobPositionMapper.insert(j);
        } else {
            jobPositionMapper.updateById(j);
        }
        return j.getId();
    }

    public void deleteJobPosition(Long id) {
        if (jobPositionMapper.deleteById(id) == 0) throw new BizException("岗位不存在：" + id);
    }

    // ── 薪资统计 ──────────────────────────────────────────────────────────────

    public PageResult<SalaryStat> pageSalaryStat(int pageNum, int pageSize, Long positionId, String city, Integer statYear) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<SalaryStat> w = new LambdaQueryWrapper<SalaryStat>()
                .eq(positionId != null, SalaryStat::getPositionId, positionId)
                .eq(StringUtils.hasText(city), SalaryStat::getCity, city)
                .eq(statYear != null, SalaryStat::getStatYear, statYear)
                .orderByDesc(SalaryStat::getStatYear)
                .orderByDesc(SalaryStat::getId);
        Page<SalaryStat> p = salaryStatMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    public Long saveSalaryStat(SalaryStat s) {
        if (s == null || s.getPositionId() == null || s.getPositionId() < 1
                || !StringUtils.hasText(s.getCity()) || !StringUtils.hasText(s.getExperience())
                || !StringUtils.hasText(s.getDegree()) || s.getStatYear() == null) {
            throw new BizException("薪资统计必须填写岗位、城市、经验、学历和统计年份");
        }
        if (s.getStatYear() < 2000 || s.getStatYear() > 2100) {
            throw new BizException("统计年份必须处于 2000-2100 之间");
        }
        if (isNegative(s.getMinSalary()) || isNegative(s.getMaxSalary())
                || isNegative(s.getMedianSalary()) || isNegative(s.getSampleSize())) {
            throw new BizException("薪资和样本量不能为负数");
        }
        if (s.getMinSalary() != null && s.getMaxSalary() != null && s.getMinSalary() > s.getMaxSalary()) {
            throw new BizException("最低薪资不能高于最高薪资");
        }
        if (s.getMedianSalary() != null
                && ((s.getMinSalary() != null && s.getMedianSalary() < s.getMinSalary())
                || (s.getMaxSalary() != null && s.getMedianSalary() > s.getMaxSalary()))) {
            throw new BizException("中位薪资必须处于最低薪资和最高薪资之间");
        }
        if (s.getId() == null) {
            salaryStatMapper.insert(s);
        } else {
            salaryStatMapper.updateById(s);
        }
        return s.getId();
    }

    public void deleteSalaryStat(Long id) {
        if (salaryStatMapper.deleteById(id) == 0) throw new BizException("薪资记录不存在：" + id);
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数非法：页码必须大于 0，页大小为 1-200");
        }
    }

    private boolean isNegative(Integer value) {
        return value != null && value < 0;
    }
}
