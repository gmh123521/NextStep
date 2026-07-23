package com.nextstep.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ns_user_profile")
public class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    private String currentSchool;
    private String schoolLevel;
    private String currentMajor;
    private String majorCategory;
    private String degreeType;
    private Integer gradeYear;
    private BigDecimal gpa;
    private Integer gpaScale;
    private BigDecimal classRankPct;

    private String englishLevel;
    private Integer englishScore;

    /** 以下 4 个 has_* 是派生字段：从 ns_user_experience 聚合得来，不持久化 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer hasResearch;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer hasInternship;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer hasCompetition;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer hasPaper;

    private String targetPaths;
    private String preferredRegions;
    private String preferredIndustries;
    private Integer salaryExpectation;

    private Integer riskAppetite;
    private Integer monthlyBudget;

    private String interests;
    private String strengths;
    private String weaknesses;

    private String currentStatus;

    /** 派生字段：实时算注入响应，不持久化到数据库 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer profileCompleteness;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
