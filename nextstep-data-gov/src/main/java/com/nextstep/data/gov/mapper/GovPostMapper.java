package com.nextstep.data.gov.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.data.gov.entity.GovPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GovPostMapper extends BaseMapper<GovPost> {

    /**
     * 岗位综合视图：合并招录 + 进面线，并计算"上岸率"（招录/通过审查）
     */
    @Select("""
        SELECT p.id, p.year, p.exam_type, p.province, p.dept_name, p.post_name, p.region,
               p.degree_required, p.major_required, p.political, p.extra_required,
               e.enroll_count, e.apply_count, e.apply_pass, e.attend_count,
               c.interview_min, c.interview_max, c.final_min,
               ROUND(e.enroll_count / NULLIF(e.apply_pass,0) * 100, 2) AS admit_rate_pct,
               ROUND(e.apply_pass  / NULLIF(e.enroll_count,0), 2)     AS apply_per_seat
        FROM ns_gov_post p
        LEFT JOIN ns_gov_enroll e ON e.post_id = p.id
        LEFT JOIN ns_gov_cutoff c ON c.post_id = p.id
        WHERE p.id = #{postId}
        """)
    Map<String,Object> selectDetail(@Param("postId") Long postId);

    @Select({"<script>",
        "SELECT p.id, p.year, p.exam_type, p.province, p.dept_name, p.post_name, p.region,",
        "       p.degree_required, p.major_required,",
        "       e.enroll_count, e.apply_pass,",
        "       ROUND(e.enroll_count / NULLIF(e.apply_pass,0) * 100, 2) AS admit_rate_pct",
        "FROM ns_gov_post p LEFT JOIN ns_gov_enroll e ON e.post_id = p.id",
        "<where>",
        "  <if test='year != null'>AND p.year = #{year}</if>",
        "  <if test='examType != null and examType != \"\"'>AND p.exam_type = #{examType}</if>",
        "  <if test='province != null and province != \"\"'>AND p.province = #{province}</if>",
        "  <if test='keyword != null and keyword != \"\"'>",
        "     AND (p.post_name LIKE CONCAT('%',#{keyword},'%') OR p.dept_name LIKE CONCAT('%',#{keyword},'%'))",
        "  </if>",
        "</where>",
        "ORDER BY p.year DESC, p.id ASC",
        "</script>"})
    List<Map<String,Object>> search(
            @Param("year") Integer year,
            @Param("examType") String examType,
            @Param("province") String province,
            @Param("keyword") String keyword);
}
