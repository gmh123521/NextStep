package com.nextstep.analysis.strategy;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 评分用的常量与权重表
 */
public final class ScoringConstants {

    private ScoringConstants() {}

    /** 院校层次基础分（0-100） */
    public static final Map<String, Integer> SCHOOL_LEVEL_SCORE = Map.of(
            "C9",           95,
            "985",          88,
            "211",          80,
            "DOUBLE_FIRST", 75,
            "REGULAR",      60,
            "COLLEGE",      45
    );

    /** 英语等级基础分 */
    public static final Map<String, Integer> ENGLISH_LEVEL_SCORE = Map.ofEntries(
            Map.entry("CET4",    65),
            Map.entry("CET6",    78),
            Map.entry("TEM4",    80),
            Map.entry("TEM8",    92),
            Map.entry("IELTS",   85),
            Map.entry("TOEFL",   85),
            Map.entry("JLPT_N1", 90),
            Map.entry("JLPT_N2", 75),
            Map.entry("JLPT_N3", 60),
            Map.entry("JLPT_N4", 45),
            Map.entry("JLPT_N5", 30),
            Map.entry("TOPIK1",  55),
            Map.entry("TOPIK2",  80),
            Map.entry("OTHER",   60),
            Map.entry("NONE",    30)
    );

    /** 默认值兜底 */
    public static final int DEFAULT_DIMENSION_SCORE = 50;

    /**
     * 把任意制式的 GPA 标准化到 4 分制（评分代码统一用这个尺度）
     *
     * 用业内通行的「WES 风格分段映射」，与每门课的 course.gpa 单门换算规则保持一致：
     *
     *   百分制 → 4 分制：
     *     90+   → 4.0    85-89 → 3.7    82-84 → 3.3
     *     78-81 → 3.0    75-77 → 2.7    72-74 → 2.3
     *     68-71 → 2.0    64-67 → 1.5    60-63 → 1.0   <60 → 0
     *
     *   5 分制 → 4 分制（先视为 5 分制每一档对应百分制相同分位的等价值）：
     *     4.5+    → 4.0    4.0-4.4 → 3.6    3.5-3.9 → 3.2
     *     3.0-3.4 → 2.7    2.5-2.9 → 2.0    <2.5    → 1.0
     *
     *   4 分制 → 原值
     *
     * 选这套规则的理由：
     *   - 与成绩单单门课绩点换算一致，用户看到 88 平均期望 ≈3.7 而非线性 3.52
     *   - 留学申请 WES、国内大学保研材料都用类似分段
     *
     * null 输入返回 0；越界值会 clamp 到 [0, 4]
     */
    public static double normalizeGpaTo4(BigDecimal gpa, Integer scale) {
        if (gpa == null) return 0d;
        int s = scale == null ? 4 : scale;
        double v = gpa.doubleValue();
        double result = switch (s) {
            case 100 -> mapPercentageToFour(v);
            case 5   -> mapFiveToFour(v);
            default  -> v;
        };
        if (result < 0) return 0d;
        if (result > 4) return 4d;
        return result;
    }

    private static double mapPercentageToFour(double pct) {
        if (pct >= 90) return 4.0;
        if (pct >= 85) return 3.7;
        if (pct >= 82) return 3.3;
        if (pct >= 78) return 3.0;
        if (pct >= 75) return 2.7;
        if (pct >= 72) return 2.3;
        if (pct >= 68) return 2.0;
        if (pct >= 64) return 1.5;
        if (pct >= 60) return 1.0;
        return 0;
    }

    private static double mapFiveToFour(double v) {
        if (v >= 4.5) return 4.0;
        if (v >= 4.0) return 3.6;
        if (v >= 3.5) return 3.2;
        if (v >= 3.0) return 2.7;
        if (v >= 2.5) return 2.0;
        return 1.0;
    }
}

