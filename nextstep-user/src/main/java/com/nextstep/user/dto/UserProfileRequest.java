package com.nextstep.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "画像创建/更新请求")
public class UserProfileRequest {

    @Schema(description = "当前院校") private String currentSchool;
    @Schema(description = "院校层次 C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE") private String schoolLevel;
    @Schema(description = "当前专业") private String currentMajor;
    @Schema(description = "学科门类") private String majorCategory;
    @Schema(description = "学历 BACHELOR/MASTER/DOCTOR") private String degreeType;

    @Min(1) @Max(7)
    @Schema(description = "年级 1-7") private Integer gradeYear;

    @DecimalMin("0.0") @DecimalMax("100.0")
    @Schema(description = "GPA") private BigDecimal gpa;

    @Schema(description = "GPA 满分 4/5/100") private Integer gpaScale;

    @DecimalMin("0.0") @DecimalMax("100.0")
    @Schema(description = "排名百分位") private BigDecimal classRankPct;

    @Schema(description = "英语等级") private String englishLevel;
    @Schema(description = "英语分数") private Integer englishScore;

    // hasResearch / hasInternship / hasCompetition / hasPaper 已改为派生字段
    // （从 ns_user_experience 实时聚合），不再通过 Request 传入

    @Schema(description = "目标路径，多选逗号分隔 PG,CS,EM") private String targetPaths;
    @Schema(description = "偏好城市") private String preferredRegions;
    @Schema(description = "偏好行业") private String preferredIndustries;
    @Schema(description = "期望月薪") private Integer salaryExpectation;

    @Min(1) @Max(5) @Schema(description = "风险偏好 1-5") private Integer riskAppetite;
    @Min(0) @Schema(description = "每月可承受备考开销（元）") private Integer monthlyBudget;

    @Schema(description = "兴趣") private String interests;
    @Schema(description = "优势") private String strengths;
    @Schema(description = "劣势") private String weaknesses;

    @Schema(description = "状态") private String currentStatus;
}
