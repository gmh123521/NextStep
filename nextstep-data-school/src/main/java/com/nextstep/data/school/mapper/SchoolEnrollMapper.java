package com.nextstep.data.school.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.data.school.entity.SchoolEnroll;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SchoolEnrollMapper extends BaseMapper<SchoolEnroll> {

    /** 计算某专业最近若干年的"录取率"（实际录取/报考人数）和报录比 */
    @Select("""
        SELECT year,
               enroll_actual,
               apply_count,
               cutoff_score,
               ROUND(enroll_actual / NULLIF(apply_count,0) * 100, 2) AS admit_rate_pct,
               ROUND(apply_count / NULLIF(enroll_actual,0), 2)      AS apply_per_seat
        FROM ns_school_enroll
        WHERE major_id = #{majorId}
        ORDER BY year DESC
        """)
    List<Map<String,Object>> selectAdmitStatsByMajor(@Param("majorId") Long majorId);
}
