package com.nextstep.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 仅用于读取经历类型聚合（派生 has_* 标志位用）
 * 故意不复用 ai 模块的 UserExperienceMapper，避免 user 模块反向依赖 ai 模块
 */
@Mapper
public interface UserExperienceTypeMapper {

    /** 返回 [{type:"INTERNSHIP", c:2}, {type:"PAPER", c:1}, ...] */
    @Select("""
            SELECT type AS type, COUNT(*) AS c
            FROM ns_user_experience
            WHERE user_id = #{userId} AND deleted = 0
            GROUP BY type
            """)
    List<Map<String, Object>> countByTypes(@Param("userId") Long userId);

    /** 拉指定类型的标题列表，用于关键词扫描（如 AWARD → 检测是否含"杯/赛"等竞赛词） */
    @Select("""
            SELECT title FROM ns_user_experience
            WHERE user_id = #{userId} AND type = #{type} AND deleted = 0
            """)
    List<String> titlesByType(@Param("userId") Long userId, @Param("type") String type);
}
