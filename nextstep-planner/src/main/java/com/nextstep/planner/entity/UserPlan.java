package com.nextstep.planner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_user_plan")
public class UserPlan implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String path;            // PG / CS / EM
    private String targetSummary;
    private String strategy;
    private Integer totalMonths;
    private String riskAlerts;      // JSON 数组字符串

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 不再使用软删除——规划是\"完全替换式\"数据，加 @TableLogic 会让旧记录占用 uk_user_path 唯一键 */
    private Integer deleted;
}
