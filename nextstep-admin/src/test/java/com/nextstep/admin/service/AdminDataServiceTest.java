package com.nextstep.admin.service;

import com.nextstep.common.exception.BizException;
import com.nextstep.data.gov.mapper.GovPostMapper;
import com.nextstep.data.job.entity.SalaryStat;
import com.nextstep.data.job.mapper.JobPositionMapper;
import com.nextstep.data.job.mapper.SalaryStatMapper;
import com.nextstep.data.school.mapper.SchoolMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AdminDataServiceTest {

    @Mock private SchoolMapper schoolMapper;
    @Mock private GovPostMapper govPostMapper;
    @Mock private JobPositionMapper jobPositionMapper;
    @Mock private SalaryStatMapper salaryStatMapper;

    @InjectMocks private AdminDataService service;

    @Test
    void rejectsSalaryStatWithoutCity() {
        SalaryStat stat = validSalaryStat();
        stat.setCity(" ");

        BizException error = assertThrows(BizException.class, () -> service.saveSalaryStat(stat));

        assertEquals("薪资统计必须填写岗位、城市、经验、学历和统计年份", error.getMessage());
    }

    @Test
    void rejectsMedianSalaryOutsideRange() {
        SalaryStat stat = validSalaryStat();
        stat.setMedianSalary(25000);

        BizException error = assertThrows(BizException.class, () -> service.saveSalaryStat(stat));

        assertEquals("中位薪资必须处于最低薪资和最高薪资之间", error.getMessage());
    }

    private SalaryStat validSalaryStat() {
        SalaryStat stat = new SalaryStat();
        stat.setPositionId(1L);
        stat.setCity("上海");
        stat.setExperience("1-3Y");
        stat.setDegree("BACHELOR");
        stat.setMinSalary(10000);
        stat.setMaxSalary(20000);
        stat.setMedianSalary(15000);
        stat.setSampleSize(100);
        stat.setStatYear(2026);
        return stat;
    }
}
