package com.nextstep.ai.service;

import com.nextstep.ai.dto.ResumeExtractResult;
import com.nextstep.common.exception.BizException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 简历确认入库前的字段规范化与完整校验。 */
final class ResumeResultValidator {

    private static final Set<String> SCHOOL_LEVELS = Set.of(
            "C9", "985", "211", "DOUBLE_FIRST", "REGULAR", "COLLEGE");
    private static final Set<String> DEGREE_TYPES = Set.of("BACHELOR", "MASTER", "DOCTOR");
    private static final Set<String> ENGLISH_LEVELS = Set.of(
            "CET4", "CET6", "TEM4", "TEM8", "IELTS", "TOEFL",
            "JLPT_N1", "JLPT_N2", "JLPT_N3", "JLPT_N4", "JLPT_N5",
            "TOPIK1", "TOPIK2", "OTHER", "NONE");
    private static final Map<String, Integer> ENGLISH_SCORE_MAX = Map.ofEntries(
            Map.entry("CET4", 710), Map.entry("CET6", 710),
            Map.entry("TEM4", 100), Map.entry("TEM8", 100),
            Map.entry("IELTS", 90), Map.entry("TOEFL", 120),
            Map.entry("JLPT_N1", 180), Map.entry("JLPT_N2", 180),
            Map.entry("JLPT_N3", 180), Map.entry("JLPT_N4", 180),
            Map.entry("JLPT_N5", 180), Map.entry("TOPIK1", 200),
            Map.entry("TOPIK2", 300));
    private static final Set<String> EXPERIENCE_TYPES = Set.of(
            "INTERNSHIP", "PROJECT", "AWARD", "RESEARCH", "PAPER", "COMPETITION");
    private static final Set<Integer> GPA_SCALES = Set.of(4, 5, 100);

    void validateAndNormalize(ResumeExtractResult result) {
        if (result == null) throw new BizException("简历解析结果不能为空");

        result.setCurrentSchool(optionalText(result.getCurrentSchool(), 128, "当前院校"));
        result.setCurrentMajor(optionalText(result.getCurrentMajor(), 128, "当前专业"));
        result.setSchoolLevel(optionalEnum(result.getSchoolLevel(), SCHOOL_LEVELS, "院校层次"));
        result.setDegreeType(optionalEnum(result.getDegreeType(), DEGREE_TYPES, "学历类型"));
        result.setEnglishLevel(optionalEnum(result.getEnglishLevel(), ENGLISH_LEVELS, "语言等级"));

        validateGradeYear(result);
        validateGpa(result);
        validateEnglishScore(result);
        normalizeNotes(result);
        normalizeExperiences(result);
    }

    private void validateGradeYear(ResumeExtractResult result) {
        Integer gradeYear = result.getGradeYear();
        if (gradeYear == null) return;
        if (gradeYear < 1 || gradeYear > 7) throw new BizException("年级必须处于 1-7 之间");
        if ("BACHELOR".equals(result.getDegreeType()) && gradeYear > 4) {
            throw new BizException("本科年级必须处于 1-4 之间");
        }
        if (("MASTER".equals(result.getDegreeType()) || "DOCTOR".equals(result.getDegreeType()))
                && gradeYear < 5) {
            throw new BizException("硕士或博士年级必须处于 5-7 之间");
        }
    }

    private void validateGpa(ResumeExtractResult result) {
        Integer scale = result.getGpaScale();
        Double gpa = result.getGpa();
        if (scale != null && !GPA_SCALES.contains(scale)) {
            throw new BizException("GPA 满分只能是 4、5 或 100");
        }
        if (gpa == null) return;
        if (!Double.isFinite(gpa) || gpa < 0) throw new BizException("GPA 不能为负数或非法数值");
        if (scale == null) throw new BizException("填写 GPA 时必须提供 GPA 满分");
        if (gpa > scale) throw new BizException("GPA 不能超过 GPA 满分 " + scale);
    }

    private void validateEnglishScore(ResumeExtractResult result) {
        Integer score = result.getEnglishScore();
        if (score == null) return;
        if (score < 0) throw new BizException("语言成绩不能为负数");
        String level = result.getEnglishLevel();
        if (level == null || "NONE".equals(level)) throw new BizException("填写语言成绩时必须选择语言等级");
        Integer max = ENGLISH_SCORE_MAX.get(level);
        if (max != null && score > max) {
            throw new BizException(level + " 成绩不能超过 " + max);
        }
    }

    private void normalizeNotes(ResumeExtractResult result) {
        List<String> notes = result.getNotes();
        if (notes == null) {
            result.setNotes(new ArrayList<>());
            return;
        }
        if (notes.size() > 3) throw new BizException("简历备注最多保留 3 条");
        List<String> normalized = new ArrayList<>();
        for (String note : notes) {
            String value = optionalText(note, 200, "简历备注");
            if (value != null) normalized.add(value);
        }
        result.setNotes(normalized);
    }

    private void normalizeExperiences(ResumeExtractResult result) {
        List<ResumeExtractResult.ExperienceItem> experiences = result.getExperiences();
        if (experiences == null) {
            result.setExperiences(new ArrayList<>());
            return;
        }
        if (experiences.size() > 50) throw new BizException("单次最多确认 50 条经历");
        for (int i = 0; i < experiences.size(); i++) {
            ResumeExtractResult.ExperienceItem item = experiences.get(i);
            int number = i + 1;
            if (item == null) throw new BizException("第 " + number + " 条经历不能为空");

            item.setType(requiredEnum(item.getType(), EXPERIENCE_TYPES,
                    "第 " + number + " 条经历类型"));
            item.setTitle(requiredText(item.getTitle(), 255,
                    "第 " + number + " 条经历标题"));
            item.setRole(optionalText(item.getRole(), 128,
                    "第 " + number + " 条经历角色"));
            item.setStartDate(optionalText(item.getStartDate(), 16,
                    "第 " + number + " 条经历开始日期"));
            item.setEndDate(optionalText(item.getEndDate(), 16,
                    "第 " + number + " 条经历结束日期"));
            item.setDescription(optionalText(item.getDescription(), 3000,
                    "第 " + number + " 条经历描述"));

            validateDate(item.getStartDate(), "第 " + number + " 条经历开始日期");
            validateDate(item.getEndDate(), "第 " + number + " 条经历结束日期");
        }
    }

    private String optionalEnum(String value, Set<String> allowed, String label) {
        String normalized = optionalText(value, 32, label);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!allowed.contains(normalized)) throw new BizException(label + "无效：" + normalized);
        return normalized;
    }

    private String requiredEnum(String value, Set<String> allowed, String label) {
        String normalized = requiredText(value, 32, label).toUpperCase();
        if (!allowed.contains(normalized)) throw new BizException(label + "无效：" + normalized);
        return normalized;
    }

    private String optionalText(String value, int maxLength, String label) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > maxLength) {
            throw new BizException(label + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String requiredText(String value, int maxLength, String label) {
        String normalized = optionalText(value, maxLength, label);
        if (normalized == null) throw new BizException(label + "不能为空");
        return normalized;
    }

    private void validateDate(String value, String label) {
        if (value == null || value.matches("\\d{4}")) return;
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new BizException(label + "格式应为 YYYY 或 YYYY-MM");
        }
        try {
            YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BizException(label + "格式应为 YYYY 或 YYYY-MM");
        }
    }
}
