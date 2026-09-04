package com.nextstep.crawler.service;

import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.dto.KaoyanCatalogRecord;
import com.nextstep.crawler.dto.KaoyanEnrollmentRecord;
import com.nextstep.crawler.mapper.SchoolEnrollUpsertMapper;
import com.nextstep.crawler.mapper.SchoolMajorUpsertMapper;
import com.nextstep.data.school.entity.SchoolEnroll;
import com.nextstep.data.school.entity.SchoolMajor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class KaoyanPublishService {

    private final SchoolMajorUpsertMapper majorMapper;
    private final SchoolEnrollUpsertMapper enrollMapper;

    @Transactional(rollbackFor = Exception.class)
    public void publish(List<KaoyanCatalogRecord> catalogs, List<KaoyanEnrollmentRecord> enrollments) {
        if (catalogs == null || enrollments == null) throw new BizException("考研发布数据不能为空");
        for (KaoyanCatalogRecord catalog : catalogs) publishCatalog(catalog);
        for (KaoyanEnrollmentRecord enrollment : enrollments) {
            validate(enrollment);
            Long majorId = majorMapper.findMajorId(enrollment.schoolCode(), enrollment.majorCode(), enrollment.year());
            if (majorId == null) {
                throw new BizException("找不到对应的院校专业：" + enrollment.schoolCode() + "/" + enrollment.majorCode());
            }
            SchoolEnroll entity = new SchoolEnroll();
            entity.setMajorId(majorId);
            entity.setSchoolId(majorMapper.findSchoolIdByCode(enrollment.schoolCode()));
            entity.setYear(enrollment.year());
            entity.setEnrollPlan(enrollment.enrollPlan());
            entity.setEnrollActual(enrollment.enrollActual());
            entity.setApplyCount(enrollment.applyCount());
            entity.setCutoffScore(enrollment.cutoffScore());
            entity.setCutoffEnglish(enrollment.cutoffEnglish());
            entity.setCutoffPolitical(enrollment.cutoffPolitical());
            entity.setLowestScore(enrollment.lowestScore());
            entity.setHighestScore(enrollment.highestScore());
            enrollMapper.upsert(entity);
        }
    }

    public void validate(KaoyanEnrollmentRecord enrollment) {
        if (enrollment == null || blank(enrollment.schoolCode()) || blank(enrollment.majorCode())) {
            throw new BizException("考研招录记录缺少院校代码或专业代码");
        }
        if (enrollment.year() < 2000 || enrollment.year() > 2100) {
            throw new BizException("考研数据年份必须处于 2000-2100 之间");
        }
        Integer[] values = {enrollment.enrollPlan(), enrollment.enrollActual(), enrollment.applyCount(),
                enrollment.cutoffScore(), enrollment.cutoffEnglish(), enrollment.cutoffPolitical(),
                enrollment.lowestScore(), enrollment.highestScore()};
        if (Arrays.stream(values).anyMatch(value -> value != null && value < 0)) {
            throw new BizException("考研招生人数和分数不能为负数");
        }
        Integer lowest = enrollment.lowestScore();
        Integer highest = enrollment.highestScore();
        Integer cutoff = enrollment.cutoffScore();
        if (lowest != null && highest != null && lowest > highest) {
            throw new BizException("最低录取分不能高于最高录取分");
        }
        if (cutoff != null && lowest != null && cutoff > lowest) {
            throw new BizException("复试线不能高于最低录取分");
        }
    }

    private void publishCatalog(KaoyanCatalogRecord catalog) {
        if (catalog == null || blank(catalog.schoolCode()) || blank(catalog.majorCode()) || blank(catalog.majorName())) {
            throw new BizException("考研专业目录缺少院校代码或专业代码/名称");
        }
        if (catalog.year() < 2000 || catalog.year() > 2100) {
            throw new BizException("考研专业目录缺少年份或年份非法");
        }
        Long schoolId = majorMapper.findSchoolIdByCode(catalog.schoolCode());
        if (schoolId == null) throw new BizException("找不到对应院校：" + catalog.schoolCode());
        SchoolMajor entity = new SchoolMajor();
        entity.setSchoolId(schoolId);
        entity.setMajorCode(catalog.majorCode());
        entity.setMajorName(catalog.majorName());
        entity.setCategory(catalog.category());
        entity.setDegreeType(catalog.degreeType());
        entity.setYear(catalog.year());
        majorMapper.upsert(entity);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
