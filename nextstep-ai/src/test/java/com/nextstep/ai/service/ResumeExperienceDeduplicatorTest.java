package com.nextstep.ai.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeExperienceDeduplicatorTest {

    private final ResumeExperienceDeduplicator deduplicator = new ResumeExperienceDeduplicator();

    @Test
    void keepsSameTitleWhenExperienceGroupsAreDifferent() {
        Set<String> fingerprints = new HashSet<>();
        Map<String, List<String>> titlesByGroup = new HashMap<>();
        deduplicator.remember(fingerprints, titlesByGroup, "AWARD", "创新项目");

        boolean duplicate = deduplicator.isDuplicate(
                fingerprints, titlesByGroup, "PROJECT", "创新项目");

        assertFalse(duplicate);
    }

    @Test
    void mergesAwardCompetitionAndPaperIntoOneDeduplicationGroup() {
        Set<String> fingerprints = new HashSet<>();
        Map<String, List<String>> titlesByGroup = new HashMap<>();
        deduplicator.remember(fingerprints, titlesByGroup, "AWARD", "蓝桥杯省级一等奖");

        assertTrue(deduplicator.isDuplicate(
                fingerprints, titlesByGroup, "COMPETITION", "蓝桥杯"));
        assertTrue(deduplicator.isDuplicate(
                fingerprints, titlesByGroup, "PAPER", "蓝桥杯省级一等奖"));
    }

    @Test
    void detectsContainedTitlesWithinSameProjectGroup() {
        Set<String> fingerprints = new HashSet<>();
        Map<String, List<String>> titlesByGroup = new HashMap<>();
        deduplicator.remember(fingerprints, titlesByGroup, "PROJECT", "校园二手交易平台项目");

        assertTrue(deduplicator.isDuplicate(
                fingerprints, titlesByGroup, "PROJECT", "校园二手交易平台"));
    }

    @Test
    void shortTitlesOnlyDeduplicateOnExactFingerprint() {
        Set<String> fingerprints = new HashSet<>();
        Map<String, List<String>> titlesByGroup = new HashMap<>();
        deduplicator.remember(fingerprints, titlesByGroup, "PROJECT", "AI");

        assertTrue(deduplicator.isDuplicate(fingerprints, titlesByGroup, "PROJECT", "AI"));
        assertFalse(deduplicator.isDuplicate(fingerprints, titlesByGroup, "PROJECT", "AI助手"));
    }
}
