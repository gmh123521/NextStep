package com.nextstep.data.school.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
import com.nextstep.data.school.entity.School;
import com.nextstep.data.school.entity.SchoolEnroll;
import com.nextstep.data.school.entity.SchoolMajor;
import com.nextstep.data.school.mapper.SchoolEnrollMapper;
import com.nextstep.data.school.mapper.SchoolMajorMapper;
import com.nextstep.data.school.mapper.SchoolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolMapper schoolMapper;
    private final SchoolMajorMapper schoolMajorMapper;
    private final SchoolEnrollMapper schoolEnrollMapper;

    public PageResult<School> page(int pageNum, int pageSize, String keyword, String level, String province) {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<School>()
                .like(StringUtils.hasText(keyword),  School::getName, keyword)
                .eq(StringUtils.hasText(level),      School::getLevel, level)
                .eq(StringUtils.hasText(province),   School::getProvince, province)
                .orderByDesc(School::getIsSelfMarking)
                .orderByAsc(School::getId);
        Page<School> p = schoolMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    public List<SchoolMajor> listMajors(Long schoolId) {
        return schoolMajorMapper.selectList(new LambdaQueryWrapper<SchoolMajor>()
                .eq(SchoolMajor::getSchoolId, schoolId));
    }

    public List<SchoolEnroll> listEnrolls(Long majorId) {
        return schoolEnrollMapper.selectList(new LambdaQueryWrapper<SchoolEnroll>()
                .eq(SchoolEnroll::getMajorId, majorId)
                .orderByDesc(SchoolEnroll::getYear));
    }

    public List<Map<String,Object>> admitStats(Long majorId) {
        return schoolEnrollMapper.selectAdmitStatsByMajor(majorId);
    }
}
