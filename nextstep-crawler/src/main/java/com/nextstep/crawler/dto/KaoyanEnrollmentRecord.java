package com.nextstep.crawler.dto;

/** 已标准化的考研年度招录和分数记录。 */
public record KaoyanEnrollmentRecord(
        String schoolCode,
        String majorCode,
        int year,
        Integer enrollPlan,
        Integer enrollActual,
        Integer applyCount,
        Integer cutoffScore,
        Integer cutoffEnglish,
        Integer cutoffPolitical,
        Integer lowestScore,
        Integer highestScore
) {
}
