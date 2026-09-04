package com.nextstep.crawler.mapper;

import com.nextstep.data.school.entity.SchoolEnroll;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SchoolEnrollUpsertMapper {

    @Insert("""
            INSERT INTO ns_school_enroll
                (school_id, major_id, year, enroll_plan, enroll_actual, apply_count,
                 cutoff_score, cutoff_english, cutoff_political, lowest_score, highest_score)
            VALUES
                (#{e.schoolId}, #{e.majorId}, #{e.year}, #{e.enrollPlan}, #{e.enrollActual}, #{e.applyCount},
                 #{e.cutoffScore}, #{e.cutoffEnglish}, #{e.cutoffPolitical}, #{e.lowestScore}, #{e.highestScore})
            ON DUPLICATE KEY UPDATE
                enroll_plan = VALUES(enroll_plan),
                enroll_actual = VALUES(enroll_actual),
                apply_count = VALUES(apply_count),
                cutoff_score = VALUES(cutoff_score),
                cutoff_english = VALUES(cutoff_english),
                cutoff_political = VALUES(cutoff_political),
                lowest_score = VALUES(lowest_score),
                highest_score = VALUES(highest_score)
            """)
    int upsert(@Param("e") SchoolEnroll enroll);
}
