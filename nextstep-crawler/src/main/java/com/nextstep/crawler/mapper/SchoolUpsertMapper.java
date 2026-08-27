package com.nextstep.crawler.mapper;

import com.nextstep.data.school.entity.School;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 采集专用写入：INSERT IGNORE 借助 ns_school 的唯一键 (name) 去重。
 * 返回受影响行数（1=新增，0=已存在）。
 */
@Mapper
public interface SchoolUpsertMapper {

    @Insert("""
        INSERT IGNORE INTO ns_school
            (name, code, province, city, level, type, is_self_marking, created_at, updated_at)
        VALUES
            (#{s.name}, #{s.code}, #{s.province}, #{s.city}, #{s.level}, #{s.type},
             #{s.isSelfMarking}, NOW(), NOW())
        """)
    int insertIgnore(@Param("s") School school);
}
