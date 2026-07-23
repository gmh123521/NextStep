package com.nextstep.planner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.planner.entity.UserPlan;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPlanMapper extends BaseMapper<UserPlan> {

    /**
     * 物理删除（绕开全局 logic-delete）
     * 规划是\"完全替换式\"数据，软删除会让旧记录占用 uk_user_path 唯一键导致重新生成失败
     */
    @Delete("DELETE FROM ns_user_plan WHERE id = #{id}")
    int physicalDelete(@Param("id") Long id);
}
