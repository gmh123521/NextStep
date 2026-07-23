package com.nextstep.planner.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 输出的结构化规划（apply 后会落库到 ns_user_plan + ns_user_plan_task）
 */
@Data
public class PlanGeneration implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 路径：PG / CS / EM */
    private String path;
    private String targetSummary;
    private String strategy;
    private Integer totalMonths;
    private List<String> riskAlerts = new ArrayList<>();
    private List<Phase> phases = new ArrayList<>();

    @Data
    public static class Phase implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String phase;        // 第1月
        private Integer phaseOrder;
        private List<Task> tasks = new ArrayList<>();
    }

    @Data
    public static class Task implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String subject;      // 数学 / 简历 / 面试
        private String title;
        private String description;
    }
}
