package com.nextstep.crawler.mapper;

import com.nextstep.data.gov.entity.GovPost;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 采集专用写入：INSERT IGNORE 借助 ns_gov_post 的唯一键
 * (year, exam_type, dept_name, post_code) 去重，返回受影响行数（1=新增，0=已存在）。
 */
@Mapper
public interface GovPostUpsertMapper {

    @Insert("""
        INSERT IGNORE INTO ns_gov_post
            (year, exam_type, province, dept_name, post_code, post_name, region,
             degree_required, major_required, political, extra_required, created_at)
        VALUES
            (#{p.year}, #{p.examType}, #{p.province}, #{p.deptName}, #{p.postCode}, #{p.postName}, #{p.region},
             #{p.degreeRequired}, #{p.majorRequired}, #{p.political}, #{p.extraRequired}, NOW())
        """)
    int insertIgnore(@Param("p") GovPost post);
}
