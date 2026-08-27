package com.nextstep.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.admin.dto.AdminStats;
import com.nextstep.auth.entity.User;
import com.nextstep.auth.mapper.UserMapper;
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

/** 后台概览统计：各资源计数 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserMapper userMapper;
    private final SchoolMapper schoolMapper;
    private final GovPostMapper govPostMapper;
    private final JobPositionMapper jobPositionMapper;
    private final SalaryStatMapper salaryStatMapper;

    public AdminStats overview() {
        AdminStats s = new AdminStats();
        s.setTotalUsers(userMapper.selectCount(null));
        s.setActiveUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 0)));
        s.setDisabledUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));
        s.setAdminUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN")));

        s.setTotalSchools(schoolMapper.selectCount(null));
        s.setTotalGovPosts(govPostMapper.selectCount(null));
        s.setTotalJobPositions(jobPositionMapper.selectCount(null));
        s.setTotalSalaryStats(salaryStatMapper.selectCount(null));
        return s;
    }
}
