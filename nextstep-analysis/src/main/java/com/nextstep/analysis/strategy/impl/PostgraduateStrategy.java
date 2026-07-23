package com.nextstep.analysis.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.strategy.PathScoreStrategy;
import com.nextstep.analysis.strategy.ScoringConstants;
import com.nextstep.data.school.entity.School;
import com.nextstep.data.school.entity.SchoolEnroll;
import com.nextstep.data.school.entity.SchoolMajor;
import com.nextstep.data.school.mapper.SchoolEnrollMapper;
import com.nextstep.data.school.mapper.SchoolMajorMapper;
import com.nextstep.data.school.mapper.SchoolMapper;
import com.nextstep.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考研路径评分
 */
@Component
@RequiredArgsConstructor
public class PostgraduateStrategy implements PathScoreStrategy {

    private final SchoolMapper schoolMapper;
    private final SchoolMajorMapper schoolMajorMapper;
    private final SchoolEnrollMapper schoolEnrollMapper;

    @Override public String code() { return "PG"; }
    @Override public String name() { return "考研"; }

    @Override
    public PathScore score(UserProfile p) {
        PathScore r = new PathScore();
        r.setPath(code()); r.setPathName(name());

        int academic   = scoreAcademic(p);
        int english    = scoreEnglish(p);
        int competition= scoreCompetition(p);
        int investment = scoreTimeInvestment(p);
        int budget     = scoreBudget(p);

        r.getDimensions().add(new PathScore.DimensionScore("学业基础", academic));
        r.getDimensions().add(new PathScore.DimensionScore("英语能力", english));
        r.getDimensions().add(new PathScore.DimensionScore("竞争力定位", competition));
        r.getDimensions().add(new PathScore.DimensionScore("时间投入", investment));
        r.getDimensions().add(new PathScore.DimensionScore("经济负担", budget));

        // 加权
        double overall = academic * 0.30 + english * 0.20 + competition * 0.25
                       + investment * 0.15 + budget * 0.10;
        r.setOverall((int) Math.round(overall));

        // 建议
        if (academic < 60) r.getAdvice().add("学业基础偏弱，建议优先提升 GPA 与院校背景认知");
        if (english  < 60) r.getAdvice().add("英语短板明显，考研英语建议从基础词汇与长难句切入");
        if (budget   < 50) r.getAdvice().add("月度预算可能影响备考稳定性，建议考虑性价比城市/在职备考");
        if (overall  >= 80) r.getAdvice().add("综合素质良好，可冲刺 985/C9 院校的核心专业");

        // 推荐
        r.setRecommendations(buildRecommendations(p, r.getOverall()));
        return r;
    }

    private int scoreAcademic(UserProfile p) {
        int base = ScoringConstants.SCHOOL_LEVEL_SCORE
                .getOrDefault(p.getSchoolLevel(), ScoringConstants.DEFAULT_DIMENSION_SCORE);
        // GPA 用统一工具标准化到 4 分制
        if (p.getGpa() != null) {
            double normalized = ScoringConstants.normalizeGpaTo4(p.getGpa(), p.getGpaScale());
            // 3.5/4.0 → +10，3.0 → 0，2.5 → -10
            int gpaBonus = (int) Math.round((normalized - 3.0) * 20);
            base = Math.max(20, Math.min(100, base + gpaBonus));
        }
        return base;
    }

    private int scoreEnglish(UserProfile p) {
        if (p.getEnglishLevel() == null) return ScoringConstants.DEFAULT_DIMENSION_SCORE;
        return ScoringConstants.ENGLISH_LEVEL_SCORE
                .getOrDefault(p.getEnglishLevel(), ScoringConstants.DEFAULT_DIMENSION_SCORE);
    }

    private int scoreCompetition(UserProfile p) {
        int s = 50;
        if (Integer.valueOf(1).equals(p.getHasResearch()))    s += 15;
        if (Integer.valueOf(1).equals(p.getHasPaper()))       s += 15;
        if (Integer.valueOf(1).equals(p.getHasCompetition())) s += 10;
        if (p.getClassRankPct() != null) {
            BigDecimal rank = p.getClassRankPct();
            s += (rank.intValue() < 10) ? 10 : (rank.intValue() < 30) ? 5 : 0;
        }
        return Math.min(100, s);
    }

    private int scoreTimeInvestment(UserProfile p) {
        if (p.getCurrentStatus() == null) return 60;
        return switch (p.getCurrentStatus()) {
            case "PREPARING"  -> 95;
            case "IN_SCHOOL"  -> 75;
            case "GRADUATED"  -> 70;
            case "JOB_HUNTING", "EMPLOYED" -> 40;
            default -> 60;
        };
    }

    private int scoreBudget(UserProfile p) {
        if (p.getMonthlyBudget() == null) return 60;
        int b = p.getMonthlyBudget();
        if (b >= 4000) return 90;
        if (b >= 2500) return 75;
        if (b >= 1500) return 60;
        if (b >= 800)  return 45;
        return 30;
    }

    /** Top 5 院校推荐：按"用户院校层次"匹配同档+冲刺档 */
    private List<PathScore.Recommendation> buildRecommendations(UserProfile p, int overall) {
        List<School> schools = schoolMapper.selectList(new LambdaQueryWrapper<>());
        if (schools.isEmpty()) return Collections.emptyList();

        return schools.stream()
                .map(s -> {
                    int sLvl = ScoringConstants.SCHOOL_LEVEL_SCORE
                            .getOrDefault(s.getLevel(), 50);
                    int diff = Math.abs(sLvl - overall);
                    int match = Math.max(0, 100 - diff);
                    PathScore.Recommendation rec = new PathScore.Recommendation();
                    rec.setType("school");
                    rec.setRefId(s.getId());
                    rec.setTitle(s.getName());
                    rec.setSubtitle(s.getProvince() + " · " + s.getCity() + " · " + s.getLevel());
                    rec.setMatchScore(match);
                    rec.setTag(sLvl > overall + 8 ? "冲" : sLvl < overall - 8 ? "保" : "稳");
                    return rec;
                })
                .sorted(Comparator.comparingInt(PathScore.Recommendation::getMatchScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}
