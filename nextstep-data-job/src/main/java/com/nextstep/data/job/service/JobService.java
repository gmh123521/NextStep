package com.nextstep.data.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.data.job.entity.Industry;
import com.nextstep.data.job.entity.JobPosition;
import com.nextstep.data.job.mapper.IndustryMapper;
import com.nextstep.data.job.mapper.JobPositionMapper;
import com.nextstep.data.job.mapper.SalaryStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobService {

    private final IndustryMapper industryMapper;
    private final JobPositionMapper jobPositionMapper;
    private final SalaryStatMapper salaryStatMapper;

    public List<Industry> listIndustries() {
        return industryMapper.selectList(null);
    }

    public List<JobPosition> listPositions(Long industryId, String keyword) {
        return jobPositionMapper.selectList(new LambdaQueryWrapper<JobPosition>()
                .eq(industryId != null,                JobPosition::getIndustryId, industryId)
                .like(StringUtils.hasText(keyword),    JobPosition::getName, keyword));
    }

    public List<Map<String,Object>> salaryByPosition(Long positionId) {
        return salaryStatMapper.selectByPosition(positionId);
    }
}
