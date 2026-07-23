package com.nextstep.analysis.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.strategy.PathScoreStrategy;
import com.nextstep.analysis.strategy.ScoringConstants;
import com.nextstep.data.job.entity.JobPosition;
import com.nextstep.data.job.mapper.JobPositionMapper;
import com.nextstep.data.job.mapper.SalaryStatMapper;
import com.nextstep.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmploymentStrategy implements PathScoreStrategy {

    private final JobPositionMapper jobPositionMapper;
    private final SalaryStatMapper salaryStatMapper;

    @Override public String code() { return "EM"; }
    @Override public String name() { return "就业"; }

    @Override
    public PathScore score(UserProfile p) {
        PathScore r = new PathScore();
        r.setPath(code()); r.setPathName(name());

        int background = scoreBackground(p);
        int experience = scoreExperience(p);
        int english    = scoreEnglish(p);
        int salaryMatch = scoreSalaryExpectation(p);
        int readiness  = scoreReadiness(p);

        r.getDimensions().add(new PathScore.DimensionScore("院校背景", background));
        r.getDimensions().add(new PathScore.DimensionScore("实战经历", experience));
        r.getDimensions().add(new PathScore.DimensionScore("外语加成", english));
        r.getDimensions().add(new PathScore.DimensionScore("薪资匹配", salaryMatch));
        r.getDimensions().add(new PathScore.DimensionScore("求职就绪", readiness));

        double overall = background * 0.20 + experience * 0.30 + english * 0.10
                       + salaryMatch * 0.20 + readiness * 0.20;
        r.setOverall((int) Math.round(overall));

        if (experience < 60) r.getAdvice().add("缺少实习/项目经历，建议优先补齐 1-2 段对口实习");
        if (salaryMatch < 60) r.getAdvice().add("期望薪资与行情存在差距，建议结合城市/学历重新评估");
        if (readiness  < 60) r.getAdvice().add("求职准备不足，建议刷新简历并启动投递");
        if (overall  >= 80)  r.getAdvice().add("综合竞争力良好，可优先冲击一二线城市头部企业");

        r.setRecommendations(buildRecommendations(p));
        return r;
    }

    private int scoreBackground(UserProfile p) {
        return ScoringConstants.SCHOOL_LEVEL_SCORE
                .getOrDefault(p.getSchoolLevel(), ScoringConstants.DEFAULT_DIMENSION_SCORE);
    }

    private int scoreExperience(UserProfile p) {
        int s = 45;
        if (Integer.valueOf(1).equals(p.getHasInternship())) s += 25;
        if (Integer.valueOf(1).equals(p.getHasCompetition())) s += 10;
        if (Integer.valueOf(1).equals(p.getHasResearch())) s += 8;
        if (Integer.valueOf(1).equals(p.getHasPaper())) s += 5;
        return Math.min(100, s);
    }

    private int scoreEnglish(UserProfile p) {
        if (p.getEnglishLevel() == null) return 50;
        return ScoringConstants.ENGLISH_LEVEL_SCORE.getOrDefault(p.getEnglishLevel(), 50);
    }

    private int scoreSalaryExpectation(UserProfile p) {
        if (p.getSalaryExpectation() == null) return 60;
        int exp = p.getSalaryExpectation();
        // 没有岗位上下文时，按经验值粗判：本科应届合理区间 8k-25k
        boolean masterUp = "MASTER".equals(p.getDegreeType()) || "DOCTOR".equals(p.getDegreeType());
        int lo = masterUp ? 12000 : 8000;
        int hi = masterUp ? 40000 : 25000;
        if (exp < lo)  return 90;        // 期望偏低，更易达成
        if (exp <= hi) return 75;        // 落在合理区间
        if (exp <= hi * 1.5) return 50;  // 偏高
        return 25;                       // 远高于行情
    }

    private int scoreReadiness(UserProfile p) {
        if (p.getCurrentStatus() == null) return 55;
        return switch (p.getCurrentStatus()) {
            case "JOB_HUNTING" -> 95;
            case "EMPLOYED"   -> 70; // 已就业意味着不急
            case "IN_SCHOOL"  -> 65;
            case "GRADUATED"  -> 70;
            case "PREPARING"  -> 35;
            default -> 55;
        };
    }

    /** Top 5 岗位推荐：偏好行业优先 + 兴趣方向 */
    private List<PathScore.Recommendation> buildRecommendations(UserProfile p) {
        List<JobPosition> all = jobPositionMapper.selectList(new LambdaQueryWrapper<>());
        if (all.isEmpty()) return Collections.emptyList();

        return all.stream()
                .map(pos -> {
                    int match = 55;
                    // 简化：用偏好行业字符串匹配
                    if (p.getPreferredIndustries() != null
                            && p.getPreferredIndustries().toLowerCase().contains(
                                    String.valueOf(pos.getCategory()).toLowerCase())) {
                        match += 20;
                    }
                    PathScore.Recommendation rec = new PathScore.Recommendation();
                    rec.setType("job");
                    rec.setRefId(pos.getId());
                    rec.setTitle(pos.getName());
                    rec.setSubtitle(pos.getCategory() + " · " + (pos.getDescription() == null ? "" : pos.getDescription()));
                    rec.setMatchScore(Math.min(100, match));
                    rec.setTag(match >= 75 ? "推荐" : "可选");
                    return rec;
                })
                .sorted(Comparator.comparingInt(PathScore.Recommendation::getMatchScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}
