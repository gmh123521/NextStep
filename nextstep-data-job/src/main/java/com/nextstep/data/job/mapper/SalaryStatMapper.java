package com.nextstep.data.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.data.job.entity.SalaryStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SalaryStatMapper extends BaseMapper<SalaryStat> {

    /** 岗位的薪资概览：按城市/学历聚合，附岗位名 */
    @Select("""
        SELECT s.position_id, p.name AS position_name, s.city, s.experience, s.degree,
               s.min_salary, s.max_salary, s.median_salary, s.sample_size, s.stat_year
        FROM ns_salary_stat s
        JOIN ns_job_position p ON p.id = s.position_id
        WHERE s.position_id = #{positionId}
        ORDER BY s.stat_year DESC, s.city, s.degree
        """)
    List<Map<String,Object>> selectByPosition(@Param("positionId") Long positionId);
}
