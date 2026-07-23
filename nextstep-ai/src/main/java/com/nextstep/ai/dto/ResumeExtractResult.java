package com.nextstep.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 从简历抽取出的结构化结果（也是前端确认页展示的格式）
 */
@Data
public class ResumeExtractResult implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 画像基础字段（与 ns_user_profile 一致） */
    private String currentSchool;
    private String schoolLevel;     // C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE
    private String currentMajor;
    private String degreeType;      // BACHELOR/MASTER/DOCTOR
    private Integer gradeYear;      // 1-7
    private Double gpa;
    private Integer gpaScale;       // 4 / 5 / 100
    private String englishLevel;    // CET4/CET6/TEM4/TEM8/IELTS/TOEFL/...
    private Integer englishScore;

    /** 经历列表 */
    private List<ExperienceItem> experiences = new ArrayList<>();

    /** LLM 抽取过程中的备注（如"未识别到 GPA"） */
    private List<String> notes = new ArrayList<>();

    @Data
    public static class ExperienceItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        /** INTERNSHIP / PROJECT / AWARD / RESEARCH / PAPER / COMPETITION */
        private String type;
        private String title;
        private String role;
        private String startDate;
        private String endDate;
        private String description;
    }
}
