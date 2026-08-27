package com.nextstep.report.service;

import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.service.AnalysisService;
import com.nextstep.common.exception.BizException;
import com.nextstep.planner.service.PlannerService;
import com.nextstep.report.dto.ReportData;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 综合报告：聚合 画像 + 三路径分析 + 推荐路径规划。
 * 分析是实时计算的（AnalysisService.analyze），规划可能不存在（用户没生成过则为 null）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserProfileService userProfileService;
    private final AnalysisService analysisService;
    private final PlannerService plannerService;

    public ReportData assemble(Long userId, String username) {
        UserProfile profile = userProfileService.get(userId);
        if (profile == null) throw new BizException("请先完善个人画像，再生成报告");

        AnalysisResult analysis = analysisService.analyze(userId);
        if (analysis == null) throw new BizException("暂时无法生成路径分析，请稍后重试");

        ReportData data = new ReportData();
        data.setUsername(username);
        data.setGeneratedAt(LocalDateTime.now());
        data.setProfile(profile);
        data.setAnalysis(analysis);

        // 推荐路径的规划：没生成过则保持 null，PDF 里降级为提示文案
        if (analysis.getTopPath() != null) {
            try {
                data.setPlan(plannerService.get(userId, analysis.getTopPath()));
            } catch (Exception e) {
                log.warn("[report] 读取规划失败（不阻塞报告）: {}", e.getMessage());
            }
        }
        return data;
    }
}
