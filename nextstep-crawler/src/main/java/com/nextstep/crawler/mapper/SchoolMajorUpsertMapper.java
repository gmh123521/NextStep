package com.nextstep.crawler.mapper;

import com.nextstep.data.school.entity.SchoolMajor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SchoolMajorUpsertMapper {

    @Select("SELECT id FROM ns_school WHERE code = #{schoolCode} LIMIT 1")
    Long findSchoolIdByCode(@Param("schoolCode") String schoolCode);

    @Select("SELECT id FROM ns_school_major m JOIN ns_school s ON s.id = m.school_id "
            + "WHERE s.code = #{schoolCode} AND m.major_code = #{majorCode} AND m.year = #{year} LIMIT 1")
    Long findMajorId(@Param("schoolCode") String schoolCode, @Param("majorCode") String majorCode,
                     @Param("year") int year);

    @Insert("""
            INSERT INTO ns_school_major
                (school_id, major_code, major_name, category, degree_type, year, created_at)
            VALUES
                (#{m.schoolId}, #{m.majorCode}, #{m.majorName}, #{m.category}, #{m.degreeType}, #{m.year}, NOW())
            ON DUPLICATE KEY UPDATE
                major_name = VALUES(major_name),
                category = VALUES(category),
                degree_type = VALUES(degree_type)
            """)
    int upsert(@Param("m") SchoolMajor major);
}
