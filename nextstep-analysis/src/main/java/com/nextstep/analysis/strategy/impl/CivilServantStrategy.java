package com.nextstep.analysis.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.strategy.PathScoreStrategy;
import com.nextstep.analysis.strategy.ScoringConstants;
import com.nextstep.data.gov.entity.GovPost;
import com.nextstep.data.gov.mapper.GovPostMapper;
import com.nextstep.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CivilServantStrategy implements PathScoreStrategy {

    private final GovPostMapper govPostMapper;

    @Override public String code() { return "CS"; }
    @Override public String name() { return "考公"; }

    @Override
    public PathScore score(UserProfile p) {
        PathScore r = new PathScore();
        r.setPath(code()); r.setPathName(name());

        int academic   = scoreAcademic(p);
        int matching   = scoreCityMatching(p);
        int competition= scoreCompetition(p);
        int investment = scoreTimeInvestment(p);
        int budget     = scoreBudget(p);

        r.getDimensions().add(new PathScore.DimensionScore("学历匹配", academic));
        r.getDimensions().add(new PathScore.DimensionScore("地域匹配", matching));
        r.getDimensions().add(new PathScore.DimensionScore("综合素养", competition));
        r.getDimensions().add(new PathScore.DimensionScore("备考投入", investment));
        r.getDimensions().add(new PathScore.DimensionScore("经济负担", budget));

        double overall = academic * 0.25 + matching * 0.20 + competition * 0.20
                       + investment * 0.25 + budget * 0.10;
        r.setOverall((int) Math.round(overall));

        if (academic   < 60) r.getAdvice().add("学历层次偏弱，可优先考虑县级或基层岗位");
        if (matching   < 60) r.getAdvice().add("偏好城市岗位较少，建议扩大地域选择范围");
        if (investment < 60) r.getAdvice().add("当前状态不利于全职备考，建议规划在职/裸辞节奏");
        if (overall  >= 80)  r.getAdvice().add("综合条件较好，可冲刺国考一线城市核心岗位");

        r.setRecommendations(buildRecommendations(p, r.getOverall()));
        return r;
    }

    private int scoreAcademic(UserProfile p) {
        int base = ScoringConstants.SCHOOL_LEVEL_SCORE
                .getOrDefault(p.getSchoolLevel(), ScoringConstants.DEFAULT_DIMENSION_SCORE);
        if ("MASTER".equals(p.getDegreeType())) base += 8;
        else if ("DOCTOR".equals(p.getDegreeType())) base += 12;
        return Math.min(100, base);
    }

    private int scoreCityMatching(UserProfile p) {
        if (!StringUtils.hasText(p.getPreferredRegions())) return 60;
        List<String> prefer = Arrays.stream(p.getPreferredRegions().split("[,，]"))
                .map(String::trim).filter(StringUtils::hasText).toList();
        if (prefer.isEmpty()) return 60;
        // 偏好越分散，可选岗位越多
        int cityCount = prefer.size();
        if (cityCount >= 4) return 90;
        if (cityCount >= 2) return 78;
        return 65;
    }

    private int scoreCompetition(UserProfile p) {
        int s = 55;
        if (Integer.valueOf(1).equals(p.getHasInternship())) s += 10;
        if (Integer.valueOf(1).equals(p.getHasCompetition())) s += 8;
        // 英语等级可作为公考综合素养的弱加成
        if (p.getEnglishLevel() != null) {
            int eng = ScoringConstants.ENGLISH_LEVEL_SCORE.getOrDefault(p.getEnglishLevel(), 50);
            s += (eng - 50) / 5;
        }
        return Math.max(0, Math.min(100, s));
    }

    private int scoreTimeInvestment(UserProfile p) {
        if (p.getCurrentStatus() == null) return 60;
        return switch (p.getCurrentStatus()) {
            case "PREPARING"  -> 95;
            case "GRADUATED"  -> 80;
            case "IN_SCHOOL"  -> 70;
            case "JOB_HUNTING" -> 45;
            case "EMPLOYED"   -> 35;
            default -> 60;
        };
    }

    private int scoreBudget(UserProfile p) {
        if (p.getMonthlyBudget() == null) return 60;
        int b = p.getMonthlyBudget();
        if (b >= 3000) return 85;
        if (b >= 1500) return 70;
        if (b >= 800)  return 55;
        return 35;
    }

    /** Top 5 岗位推荐：按偏好城市过滤、按上岸率排序 */
    private List<PathScore.Recommendation> buildRecommendations(UserProfile p, int overall) {
        List<GovPost> all = govPostMapper.selectList(new LambdaQueryWrapper<>());
        if (all.isEmpty()) return Collections.emptyList();

        Set<String> preferCities = StringUtils.hasText(p.getPreferredRegions())
                ? Arrays.stream(p.getPreferredRegions().split("[,，]"))
                    .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toSet())
                : Set.of();

        return all.stream()
                .map(post -> {
                    Map<String,Object> detail = govPostMapper.selectDetail(post.getId());
                    if (detail == null) return null;

                    int match = 50;
                    if (!preferCities.isEmpty() && post.getRegion() != null
                            && preferCities.stream().anyMatch(c -> post.getRegion().contains(c))) {
                        match += 25;
                    }
                    Object rate = detail.get("admit_rate_pct");
                    double rateD = rate instanceof Number n ? n.doubleValue() : 0d;
                    match += (int) Math.min(20, rateD * 5);  // 上岸率高，匹配分高

                    // 学历约束
                    if ("MASTER".equals(post.getDegreeRequired()) && "BACHELOR".equals(p.getDegreeType())) {
                        match -= 25;
                    }

                    PathScore.Recommendation rec = new PathScore.Recommendation();
                    rec.setType("gov");
                    rec.setRefId(post.getId());
                    rec.setTitle(post.getPostName());
                    rec.setSubtitle(post.getDeptName() + " · " + post.getRegion()
                            + " · 上岸率 " + (rate != null ? rate : "—") + "%");
                    rec.setMatchScore(Math.max(0, Math.min(100, match)));
                    rec.setTag(rateD < 1 ? "卷" : rateD < 3 ? "中" : "稳");
                    return rec;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PathScore.Recommendation::getMatchScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}
