package com.nextstep.report.dto;

import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.planner.dto.PlanView;
import com.nextstep.user.entity.UserProfile;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 综合报告数据：画像 + 三路径分析 + 当前规划（可能为 null） */
@Data
public class ReportData implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private String username;
    private LocalDateTime generatedAt;

    private UserProfile profile;
    private AnalysisResult analysis;

    /** 推荐路径的规划（用户可能还没生成，为 null） */
    private PlanView plan;
}
