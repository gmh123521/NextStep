package com.nextstep.planner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_user_plan_task")
public class UserPlanTask implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;
    private Long userId;

    private String phase;           // 第1月 / Month 1-2
    private Integer phaseOrder;     // 阶段排序
    private String subject;         // 数学 / 简历 / 面试（可选）
    private String title;
    private String description;
    private Integer orderIdx;       // 同阶段内排序
    private Integer completed;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    /** 不用软删除（任务随 plan 整体重建）*/
    private Integer deleted;
}
