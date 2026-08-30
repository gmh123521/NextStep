package com.nextstep.ai.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 简历经历去重规则：只在同一语义分组内比较规范化标题。 */
final class ResumeExperienceDeduplicator {

    boolean isDuplicate(Set<String> fingerprints, Map<String, List<String>> titlesByGroup,
                        String type, String title) {
        String fingerprint = fingerprint(type, title);
        if (fingerprints.contains(fingerprint)) return true;

        String normalized = normalizeTitle(title);
        if (normalized.length() < 3) return false;
        for (String old : titlesByGroup.getOrDefault(groupKey(type), List.of())) {
            if (old.length() < 3) continue;
            if (old.contains(normalized) || normalized.contains(old)) return true;
        }
        return false;
    }

    void remember(Set<String> fingerprints, Map<String, List<String>> titlesByGroup,
                  String type, String title) {
        fingerprints.add(fingerprint(type, title));
        String normalized = normalizeTitle(title);
        if (!normalized.isBlank()) {
            titlesByGroup.computeIfAbsent(groupKey(type), ignored -> new java.util.ArrayList<>())
                    .add(normalized);
        }
    }

    private String fingerprint(String type, String title) {
        return groupKey(type) + "|" + normalizeTitle(title);
    }

    private String groupKey(String type) {
        if (type == null) return "";
        return switch (type) {
            case "AWARD", "COMPETITION", "PAPER" -> "AWARD_GROUP";
            default -> type;
        };
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.toLowerCase()
                .replaceAll("[\\s\\-_·\\.,()\\[\\]【】（）/／、，。]+", "");
    }
}
