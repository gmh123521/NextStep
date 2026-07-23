package com.nextstep.planner.dto;

import com.nextstep.planner.entity.UserPlanTask;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 前端展示用：主表信息 + 按 phase 分组的任务列表 + 完成进度
 */
@Data
public class PlanView implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Long planId;
    private String path;
    private String pathName;
    private String targetSummary;
    private String strategy;
    private Integer totalMonths;
    private List<String> riskAlerts = new ArrayList<>();
    private LocalDateTime updatedAt;

    /** 按 phase 分组的任务 */
    private List<PhaseGroup> phases = new ArrayList<>();

    /** 完成进度（0-100） */
    private int progressPct;
    private int totalTasks;
    private int doneTasks;

    @Data
    public static class PhaseGroup implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String phase;
        private Integer phaseOrder;
        private List<UserPlanTask> tasks = new ArrayList<>();
    }
}
