package com.nextstep.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 多模态识别成绩单后的结构化结果
 */
@Data
public class TranscriptExtractResult implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String studentName;
    private String studentId;
    private String schoolName;
    private String majorName;

    /** 由 LLM 抽取或后端按 4 分制重算的 GPA */
    private BigDecimal computedGpa;

    /** GPA 制式（4 / 5 / 100） */
    private Integer gpaScale;

    /** 成绩单上写 GPA 那行的原文，用于审计 */
    private String officialGpaText;

    /** 总学分 */
    private BigDecimal totalCredit;

    private List<CourseItem> courses = new ArrayList<>();

    /** 抽取过程的备注（中文） */
    private List<String> notes = new ArrayList<>();

    @Data
    public static class CourseItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String courseName;
        private BigDecimal credit;
        private BigDecimal score;
        private BigDecimal gpa;
        private String semester;
        private String category;
    }
}
