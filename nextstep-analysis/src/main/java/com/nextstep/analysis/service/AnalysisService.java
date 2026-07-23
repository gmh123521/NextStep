package com.nextstep.analysis.service;

import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.strategy.PathScoreStrategy;
import com.nextstep.common.core.ResultCode;
import com.nextstep.common.exception.BizException;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserProfileService userProfileService;
    private final List<PathScoreStrategy> strategies;

    public AnalysisResult analyze(Long userId) {
        UserProfile profile = userProfileService.get(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "请先完善个人画像");
        }

        // 用户没选目标路径则默认评全部三条
        Set<String> wanted = StringUtils.hasText(profile.getTargetPaths())
                ? Arrays.stream(profile.getTargetPaths().split(","))
                    .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toSet())
                : Set.of("PG", "CS", "EM");

        List<PathScore> scores = strategies.stream()
                .filter(s -> wanted.contains(s.code()))
                .map(s -> s.score(profile))
                .sorted(Comparator.comparingInt(PathScore::getOverall).reversed())
                .collect(Collectors.toList());

        AnalysisResult r = new AnalysisResult();
        r.setUserId(userId);
        r.setAnalyzedAt(LocalDateTime.now());
        r.setProfileCompleteness(profile.getProfileCompleteness() == null ? 0 : profile.getProfileCompleteness());
        r.setPaths(scores);

        if (!scores.isEmpty()) {
            PathScore top = scores.get(0);
            r.setTopPath(top.getPath());
            r.setTopPathReason(buildTopReason(top, scores));
        }
        return r;
    }

    private String buildTopReason(PathScore top, List<PathScore> all) {
        if (all.size() == 1) {
            return top.getPathName() + "是你选择的唯一路径，综合分 " + top.getOverall();
        }
        PathScore second = all.get(1);
        int diff = top.getOverall() - second.getOverall();
        if (diff >= 10) {
            return top.getPathName() + "综合分 " + top.getOverall() + "，显著高于" + second.getPathName()
                    + "（" + second.getOverall() + "），建议作为主线";
        }
        return top.getPathName() + "微弱领先，与" + second.getPathName() + "差距较小，可并行准备";
    }
}
