package com.nextstep.user.service;

import com.nextstep.common.exception.BizException;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 用户画像所有写入入口共用的规范化与一致性校验。 */
final class UserProfileValidator {

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
    private static final Set<String> CURRENT_STATUSES = Set.of(
            "IN_SCHOOL", "PREPARING", "JOB_HUNTING", "GRADUATED", "EMPLOYED");
    private static final Set<String> TARGET_PATHS = Set.of("PG", "CS", "EM");
    private static final Set<Integer> GPA_SCALES = Set.of(4, 5, 100);

    void normalizeRequest(UserProfileRequest request) {
        if (request == null) throw new BizException("画像更新内容不能为空");

        request.setCurrentSchool(optionalText(request.getCurrentSchool(), 128, "当前院校"));
        request.setSchoolLevel(optionalEnum(request.getSchoolLevel(), SCHOOL_LEVELS, "院校层次"));
        request.setCurrentMajor(optionalText(request.getCurrentMajor(), 128, "当前专业"));
        request.setMajorCategory(optionalText(request.getMajorCategory(), 32, "学科门类"));
        request.setDegreeType(optionalEnum(request.getDegreeType(), DEGREE_TYPES, "学历类型"));
        request.setEnglishLevel(optionalEnum(request.getEnglishLevel(), ENGLISH_LEVELS, "语言等级"));
        request.setTargetPaths(normalizeTargetPaths(request.getTargetPaths()));
        request.setPreferredRegions(optionalText(request.getPreferredRegions(), 255, "偏好城市"));
        request.setPreferredIndustries(optionalText(request.getPreferredIndustries(), 255, "偏好行业"));
        request.setInterests(optionalText(request.getInterests(), 512, "兴趣描述"));
        request.setStrengths(optionalText(request.getStrengths(), 512, "优势描述"));
        request.setWeaknesses(optionalText(request.getWeaknesses(), 512, "劣势描述"));
        request.setCurrentStatus(optionalEnum(request.getCurrentStatus(), CURRENT_STATUSES, "当前状态"));
    }

    void validateProfile(UserProfile profile) {
        if (profile == null) throw new BizException("用户画像不能为空");

        requireAllowed(profile.getSchoolLevel(), SCHOOL_LEVELS, "院校层次");
        requireAllowed(profile.getDegreeType(), DEGREE_TYPES, "学历类型");
        requireAllowed(profile.getEnglishLevel(), ENGLISH_LEVELS, "语言等级");
        requireAllowed(profile.getCurrentStatus(), CURRENT_STATUSES, "当前状态");
        validateTargetPaths(profile.getTargetPaths());
        validateGradeYear(profile);
        validateGpa(profile);
        validateEnglishScore(profile);
        validateRange(profile.getClassRankPct(), BigDecimal.ZERO, new BigDecimal("100"),
                "班级排名百分位必须处于 0-100 之间");

        if (profile.getRiskAppetite() != null
                && (profile.getRiskAppetite() < 1 || profile.getRiskAppetite() > 5)) {
            throw new BizException("风险偏好必须处于 1-5 之间");
        }
        if (profile.getSalaryExpectation() != null && profile.getSalaryExpectation() < 0) {
            throw new BizException("期望月薪不能为负数");
        }
        if (profile.getMonthlyBudget() != null && profile.getMonthlyBudget() < 0) {
            throw new BizException("每月预算不能为负数");
        }
    }

    private void validateGradeYear(UserProfile profile) {
        Integer gradeYear = profile.getGradeYear();
        if (gradeYear == null) return;
        if (gradeYear < 1 || gradeYear > 7) throw new BizException("年级必须处于 1-7 之间");
        if ("BACHELOR".equals(profile.getDegreeType()) && gradeYear > 4) {
            throw new BizException("本科年级必须处于 1-4 之间");
        }
        if (("MASTER".equals(profile.getDegreeType()) || "DOCTOR".equals(profile.getDegreeType()))
                && gradeYear < 5) {
            throw new BizException("硕士或博士年级必须处于 5-7 之间");
        }
    }

    private void validateGpa(UserProfile profile) {
        Integer scale = profile.getGpaScale();
        BigDecimal gpa = profile.getGpa();
        if (scale != null && !GPA_SCALES.contains(scale)) {
            throw new BizException("GPA 满分只能是 4、5 或 100");
        }
        if (gpa == null) return;
        if (gpa.signum() < 0) throw new BizException("GPA 不能为负数");
        if (scale == null) throw new BizException("填写 GPA 时必须提供 GPA 满分");
        if (gpa.compareTo(BigDecimal.valueOf(scale)) > 0) {
            throw new BizException("GPA 不能超过 GPA 满分 " + scale);
        }
    }

    private void validateEnglishScore(UserProfile profile) {
        Integer score = profile.getEnglishScore();
        if (score == null) return;
        if (score < 0) throw new BizException("语言成绩不能为负数");
        String level = profile.getEnglishLevel();
        if (level == null || "NONE".equals(level)) {
            throw new BizException("填写语言成绩时必须选择语言等级");
        }
        Integer max = ENGLISH_SCORE_MAX.get(level);
        if (max != null && score > max) throw new BizException(level + " 成绩不能超过 " + max);
    }

    private String normalizeTargetPaths(String value) {
        String normalized = optionalText(value, 64, "目标路径");
        if (normalized == null) return null;
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (String part : normalized.split(",")) {
            String path = part.trim().toUpperCase();
            if (path.isEmpty()) continue;
            if (!TARGET_PATHS.contains(path)) throw new BizException("目标路径无效：" + path);
            paths.add(path);
        }
        return paths.isEmpty() ? null : String.join(",", paths);
    }

    private void validateTargetPaths(String value) {
        if (value == null || value.isBlank()) return;
        for (String part : value.split(",")) {
            String path = part.trim().toUpperCase();
            if (!TARGET_PATHS.contains(path)) throw new BizException("目标路径无效：" + path);
        }
    }

    private String optionalEnum(String value, Set<String> allowed, String label) {
        String normalized = optionalText(value, 32, label);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!allowed.contains(normalized)) throw new BizException(label + "无效：" + normalized);
        return normalized;
    }

    private void requireAllowed(String value, Set<String> allowed, String label) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw new BizException(label + "无效：" + value);
        }
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

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String message) {
        if (value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0)) {
            throw new BizException(message);
        }
    }
}
