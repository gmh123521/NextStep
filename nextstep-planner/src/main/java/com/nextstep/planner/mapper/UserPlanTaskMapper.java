package com.nextstep.planner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.planner.entity.UserPlanTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPlanTaskMapper extends BaseMapper<UserPlanTask> {

    /** 物理删除某 plan 下所有任务（绕开全局 logic-delete） */
    @Delete("DELETE FROM ns_user_plan_task WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);
}
