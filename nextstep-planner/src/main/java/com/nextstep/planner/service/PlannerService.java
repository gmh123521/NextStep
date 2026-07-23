package com.nextstep.planner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.ai.client.ChatModels.Message;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.service.AnalysisService;
import com.nextstep.common.core.ResultCode;
import com.nextstep.common.exception.BizException;
import com.nextstep.planner.dto.PlanGeneration;
import com.nextstep.planner.dto.PlanView;
import com.nextstep.planner.entity.UserPlan;
import com.nextstep.planner.entity.UserPlanTask;
import com.nextstep.planner.mapper.UserPlanMapper;
import com.nextstep.planner.mapper.UserPlanTaskMapper;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerService {

    private final DashScopeClient llm;
    private final UserProfileService userProfileService;
    private final AnalysisService analysisService;
    private final UserPlanMapper planMapper;
    private final UserPlanTaskMapper taskMapper;
    private final ObjectMapper om = new ObjectMapper();

    private static final Set<String> ALLOWED_PATHS = Set.of("PG", "CS", "EM");

    private static final String SYSTEM_PROMPT = """
            你是 NextStep 的规划专家，给即将上岸的学弟学妹做"可执行备考/求职计划"。

            【输出要求】
            1. 严格输出一个 JSON 对象，禁止 Markdown 代码块、禁止任何解释文字
            2. 全部用中文，自然语言不要鸡汤（"加油""你一定能"等）
            3. 任务要"可执行可衡量"，禁止说"提升综合素质"这种空话
               例：考研数学 → 第1月：完成《张宇基础30讲》前3章 + 课后习题；
                              第2月：660题 选择题部分，每天 30 题
            4. description 字段给出具体资料/教材/平台/数量（如"汤家凤接力题典 1800 题"），50-150 字

            【战线长度】
            必须严格按照用户在 user 消息中给出的 totalMonths 值生成对应阶段。

            【阶段划分规则（关键）】
            - 短战线 ≤ 9 个月：按"月"划分阶段，phase = "第1月""第2月"...
              每月 3-5 个任务，phase_order 从 1 递增
              **总任务数控制在 18-36 之间**
            - 长战线 ≥ 12 个月：按"学期/学年"划分阶段，**总阶段数严格控制在 4-6 个**，phase 用类似：
              "第1学期（基础）""第1暑假（攒经历）""第2学期（深化）""大四秋（冲刺）"等
              每阶段 4-6 个任务
              **总任务数严格控制在 20-30 之间**
              关键：长战线规划不是"短战线拉长"，而是侧重"打基础 + 试错 + 积累"
            - 严禁 12 月规划给出 12 阶段（认知负担太重）
            - 严禁 24 月规划给出 24 阶段（输出体量爆炸 + 用户看不完）

            【按路径的科目分类（subject 字段）】
            - PG：数学、英语、政治、专业课
            - CS：行测、申论、面试、专业课
            - EM：简历、技术准备、笔试刷题、项目准备、面试演练
            - 长战线（≥12 月）的早期阶段还可有：选校选岗、能力盘点、决策准备

            【风险预警 riskAlerts（最多 3 条）】
            - 基于用户画像和评分，找出最关键的 1-3 个短板
              例："你六级 150 分远低于及格，需 2 个月内冲到 425+ 否则考研英语单科危险"
              例："学校层次为普通本科，简历投大厂前需 1 段头部互联网实习"
            - 必须基于事实，不要编造
            - 每条 ≤80 字

            【strategy 总体策略（200-280 字）】
            根据用户画像 + 评分 + 战线长度，给出整体节奏建议：先突破哪门、风险点在哪、月度大致目标

            【JSON 结构】
            {
              "targetSummary": "string，一句话目标",
              "strategy": "string，200-280 字总体策略",
              "totalMonths": int，必须等于 user 消息给的值,
              "riskAlerts": ["string", ...],
              "phases": [
                {
                  "phase": "第1月",
                  "phaseOrder": 1,
                  "tasks": [
                    {"subject": "数学", "title": "...", "description": "..."}
                  ]
                }
              ]
            }

            【硬性禁忌】
            - 严禁 Markdown 代码块、JSON 注释、首尾解释文字
            - 严禁单条 task 描述少于 30 字
            - 严禁"提升综合能力""加强基础"等空话
            - 严禁同一阶段超过 7 个 task
            - 严禁 totalMonths 与 user 给定值不一致
            """;

    /** 生成（或重新生成）某条路径的规划 */
    @Transactional
    public PlanView generate(Long userId, String path) {
        return generate(userId, path, null);
    }

    /**
     * 生成规划（重载）—— months 为 null 时使用智能推断的默认值，否则用用户指定的值
     */
    @Transactional
    public PlanView generate(Long userId, String path, Integer months) {
        if (path == null || !ALLOWED_PATHS.contains(path)) {
            throw new BizException("路径必须是 PG / CS / EM 之一");
        }
        UserProfile profile = userProfileService.get(userId);
        if (profile == null) throw new BizException(ResultCode.NOT_FOUND, "请先完善个人画像");

        // 取该路径的评分（如果用户的 targetPaths 里没勾选该 path，临时强制评一下）
        PathScore pathScore = findPathScore(userId, path);

        // 决定战线长度：用户指定 → 限制在 3-24 月内；否则智能推断
        int finalMonths = (months != null) ? Math.max(3, Math.min(24, months))
                                           : recommendMonthsValue(profile, path);

        String userPrompt = buildUserPrompt(profile, path, pathScore, finalMonths);
        log.info("[planner] 调用 LLM 生成 {} 规划（{} 个月）", path, finalMonths);
        long start = System.currentTimeMillis();
        String json;
        try {
            // 给规划生成 8000 tokens（默认 4096 在 24 月长战线下会被截断导致 LLM 反复尝试）
            json = llm.chatWithMaxTokens(List.of(
                    new Message("system", SYSTEM_PROMPT),
                    new Message("user", userPrompt)
            ), 8000);
        } catch (Exception e) {
            log.warn("[planner] LLM 首次失败重试: {}", e.getMessage());
            try {
                json = llm.chatWithMaxTokens(List.of(
                        new Message("system", SYSTEM_PROMPT),
                        new Message("user", userPrompt)
                ), 8000);
            } catch (Exception e2) {
                throw new BizException("AI 规划生成超时或失败，请稍后重试");
            }
        }
        log.info("[planner] LLM 返回，耗时 {}ms", System.currentTimeMillis() - start);

        PlanGeneration gen = parseLlmJson(json, path);

        // 物理删旧规划及其任务（绕过全局 logic-delete，否则旧记录占用 uk_user_path 唯一键）
        UserPlan existing = findRawPlan(userId, path);
        if (existing != null) {
            taskMapper.physicalDeleteByPlanId(existing.getId());
            planMapper.physicalDelete(existing.getId());
        }

        // 落库主表（totalMonths 用最终决定的值，避免 LLM 偷偷改了）
        UserPlan plan = new UserPlan();
        plan.setUserId(userId);
        plan.setPath(path);
        plan.setTargetSummary(gen.getTargetSummary());
        plan.setStrategy(gen.getStrategy());
        plan.setTotalMonths(finalMonths);
        try {
            plan.setRiskAlerts(om.writeValueAsString(
                    gen.getRiskAlerts() == null ? List.of() : gen.getRiskAlerts()));
        } catch (Exception ignore) { plan.setRiskAlerts("[]"); }
        planMapper.insert(plan);

        // 落库任务
        if (gen.getPhases() != null) {
            for (PlanGeneration.Phase phase : gen.getPhases()) {
                if (phase.getTasks() == null) continue;
                int idx = 0;
                for (PlanGeneration.Task t : phase.getTasks()) {
                    if (t.getTitle() == null || t.getTitle().isBlank()) continue;
                    UserPlanTask task = new UserPlanTask();
                    task.setPlanId(plan.getId());
                    task.setUserId(userId);
                    task.setPhase(phase.getPhase());
                    task.setPhaseOrder(phase.getPhaseOrder() == null ? 0 : phase.getPhaseOrder());
                    task.setSubject(t.getSubject());
                    task.setTitle(t.getTitle());
                    task.setDescription(t.getDescription());
                    task.setOrderIdx(idx++);
                    task.setCompleted(0);
                    task.setCreatedAt(LocalDateTime.now());
                    taskMapper.insert(task);
                }
            }
        }

        return get(userId, path);
    }

    /** 给前端的"推荐战线"接口：返回推荐月数 + 推断理由（让用户知道为什么是这个值） */
    public Map<String, Object> recommendMonths(Long userId, String path) {
        if (path == null || !ALLOWED_PATHS.contains(path)) {
            throw new BizException("路径必须是 PG / CS / EM 之一");
        }
        UserProfile p = userProfileService.get(userId);
        int months = recommendMonthsValue(p, path);
        String reason = recommendReason(p, path, months);
        return Map.of("months", months, "reason", reason);
    }

    /** 智能推断：根据年级和当前状态决定推荐战线 */
    private int recommendMonthsValue(UserProfile p, String path) {
        if (p == null) return defaultStandardMonths(path);
        Integer year = p.getGradeYear();
        String status = p.getCurrentStatus();

        // 大一大二：长期战略规划（不是冲刺）
        if (year != null && year <= 2) {
            return "EM".equals(path) ? 18 : 24;
        }
        // 全职备考 / 已毕业未就业：战线可以长一些
        if ("PREPARING".equals(status) || "GRADUATED".equals(status)) {
            return switch (path) {
                case "EM" -> 6;
                case "CS" -> 9;
                default -> 10;  // PG 二战
            };
        }
        // 大三：标准战线
        if (year != null && year == 3) {
            return switch (path) {
                case "EM" -> 6;
                case "CS" -> 7;
                default -> 9;
            };
        }
        // 大四 / 应届硕博 / 默认：冲刺战线
        return defaultStandardMonths(path);
    }

    private int defaultStandardMonths(String path) {
        return switch (path) {
            case "EM" -> 4;
            case "CS" -> 5;
            default -> 6;
        };
    }

    private String recommendReason(UserProfile p, String path, int months) {
        if (p == null) return "按行业标准战线推荐";
        Integer year = p.getGradeYear();
        String status = p.getCurrentStatus();
        String pathName = pathName(path);

        if ("PREPARING".equals(status) || "GRADUATED".equals(status)) {
            return "全职备考期，给到长战线 " + months + " 个月，按月稳扎稳打";
        }
        if (year == null) return "按行业标准战线推荐 " + months + " 个月";
        if (year <= 2) {
            return "你才大" + numToCn(year) + "，距" + pathName + "还远，建议长线积累："
                    + months + " 个月战略规划（侧重打基础 + 攒经历）";
        }
        if (year == 3) {
            return "大三是" + pathName + "黄金准备期，标准战线 " + months + " 个月";
        }
        return "大四 / 应届，冲刺战线 " + months + " 个月";
    }

    private String numToCn(int n) {
        return switch (n) { case 1 -> "一"; case 2 -> "二"; case 3 -> "三"; case 4 -> "四"; default -> String.valueOf(n); };
    }

    /** 查询某路径的当前规划 */
    public PlanView get(Long userId, String path) {
        UserPlan plan = findRawPlan(userId, path);
        if (plan == null) return null;

        List<UserPlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<UserPlanTask>()
                .eq(UserPlanTask::getPlanId, plan.getId())
                .orderByAsc(UserPlanTask::getPhaseOrder)
                .orderByAsc(UserPlanTask::getOrderIdx));

        PlanView v = new PlanView();
        v.setPlanId(plan.getId());
        v.setPath(plan.getPath());
        v.setPathName(pathName(plan.getPath()));
        v.setTargetSummary(plan.getTargetSummary());
        v.setStrategy(plan.getStrategy());
        v.setTotalMonths(plan.getTotalMonths());
        v.setUpdatedAt(plan.getUpdatedAt());
        try {
            if (plan.getRiskAlerts() != null && !plan.getRiskAlerts().isBlank()) {
                v.setRiskAlerts(om.readValue(plan.getRiskAlerts(),
                        om.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
        } catch (Exception ignore) {}

        // 按 phase 分组
        Map<String, PlanView.PhaseGroup> groupMap = new LinkedHashMap<>();
        for (UserPlanTask t : tasks) {
            PlanView.PhaseGroup g = groupMap.computeIfAbsent(t.getPhase(), k -> {
                PlanView.PhaseGroup pg = new PlanView.PhaseGroup();
                pg.setPhase(k);
                pg.setPhaseOrder(t.getPhaseOrder());
                return pg;
            });
            g.getTasks().add(t);
        }
        v.setPhases(new ArrayList<>(groupMap.values()));
        v.setTotalTasks(tasks.size());
        v.setDoneTasks((int) tasks.stream().filter(t -> Integer.valueOf(1).equals(t.getCompleted())).count());
        v.setProgressPct(tasks.isEmpty() ? 0 : Math.round(100f * v.getDoneTasks() / v.getTotalTasks()));

        return v;
    }

    /** 勾选/取消勾选某条任务 */
    public void toggleTask(Long userId, Long taskId, boolean completed) {
        UserPlanTask t = taskMapper.selectById(taskId);
        if (t == null || !t.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在");
        }
        t.setCompleted(completed ? 1 : 0);
        t.setCompletedAt(completed ? LocalDateTime.now() : null);
        taskMapper.updateById(t);
    }

    // ─────────── helpers ───────────

    private UserPlan findRawPlan(Long userId, String path) {
        return planMapper.selectOne(new LambdaQueryWrapper<UserPlan>()
                .eq(UserPlan::getUserId, userId)
                .eq(UserPlan::getPath, path));
    }

    private PathScore findPathScore(Long userId, String path) {
        try {
            AnalysisResult r = analysisService.analyze(userId);
            if (r == null || r.getPaths() == null) return null;
            return r.getPaths().stream()
                    .filter(p -> path.equals(p.getPath()))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private int defaultMonths(String path) {
        return defaultStandardMonths(path);
    }

    private String pathName(String path) {
        return switch (path) {
            case "PG" -> "考研";
            case "CS" -> "考公";
            case "EM" -> "就业";
            default -> path;
        };
    }

    private String buildUserPrompt(UserProfile p, String path, PathScore score, int totalMonths) {
        StringBuilder sb = new StringBuilder();
        sb.append("【目标路径】").append(path).append("（").append(pathName(path)).append("）\n");
        sb.append("【战线长度（必须严格按这个值生成 totalMonths 和阶段数）】 ").append(totalMonths).append(" 个月");
        if (totalMonths >= 12) {
            sb.append("（长战线，请按学期/学年划分阶段，重战略而非月度细节）");
        } else {
            sb.append("（短战线，请按月划分阶段）");
        }
        sb.append("\n\n");
        sb.append("【用户画像】\n");
        appendIf(sb, "院校", p.getCurrentSchool());
        appendIf(sb, "院校层次", p.getSchoolLevel());
        appendIf(sb, "专业", p.getCurrentMajor());
        appendIf(sb, "学科门类", p.getMajorCategory());
        appendIf(sb, "学历", p.getDegreeType());
        if (p.getGradeYear() != null) sb.append("- 年级: ").append(p.getGradeYear()).append("\n");
        if (p.getGpa() != null) sb.append("- GPA: ").append(p.getGpa())
                .append(" / ").append(p.getGpaScale() == null ? 4 : p.getGpaScale()).append("\n");
        if (p.getClassRankPct() != null) sb.append("- 班级排名百分位: 前 ").append(p.getClassRankPct()).append("%\n");
        appendIf(sb, "语言等级", p.getEnglishLevel());
        if (p.getEnglishScore() != null) sb.append("- 语言分数: ").append(p.getEnglishScore()).append("\n");
        appendIf(sb, "目标路径", p.getTargetPaths());
        appendIf(sb, "偏好城市", p.getPreferredRegions());
        appendIf(sb, "偏好行业", p.getPreferredIndustries());
        if (p.getSalaryExpectation() != null) sb.append("- 期望月薪: ").append(p.getSalaryExpectation()).append("\n");
        if (p.getRiskAppetite() != null) sb.append("- 风险偏好: ").append(p.getRiskAppetite()).append("/5\n");
        if (p.getMonthlyBudget() != null) sb.append("- 月度备考预算: ").append(p.getMonthlyBudget()).append("\n");
        appendIf(sb, "当前状态", p.getCurrentStatus());
        appendIf(sb, "兴趣", p.getInterests());
        appendIf(sb, "优势", p.getStrengths());
        appendIf(sb, "短板", p.getWeaknesses());

        // 经历类型（派生）
        List<String> haveExp = new ArrayList<>();
        if (Integer.valueOf(1).equals(p.getHasInternship()))  haveExp.add("实习");
        if (Integer.valueOf(1).equals(p.getHasResearch()))    haveExp.add("科研");
        if (Integer.valueOf(1).equals(p.getHasCompetition())) haveExp.add("竞赛");
        if (Integer.valueOf(1).equals(p.getHasPaper()))       haveExp.add("论文");
        if (!haveExp.isEmpty()) sb.append("- 已有经历类型: ").append(String.join("、", haveExp)).append("\n");

        if (score != null) {
            sb.append("\n【该路径评分（务必参照短板维度做规划）】\n");
            sb.append("- 综合分: ").append(score.getOverall()).append("/100\n");
            if (score.getDimensions() != null) {
                sb.append("- 维度: ").append(score.getDimensions().stream()
                        .map(d -> d.getName() + " " + d.getScore())
                        .collect(Collectors.joining("、"))).append("\n");
            }
        }
        sb.append("\n现在请按 system 指示生成 JSON 规划。");
        return sb.toString();
    }

    private void appendIf(StringBuilder sb, String label, String v) {
        if (v != null && !v.isBlank()) sb.append("- ").append(label).append(": ").append(v).append("\n");
    }

    private PlanGeneration parseLlmJson(String raw, String fallbackPath) {
        String json = stripCodeFence(raw);
        try {
            PlanGeneration g = om.readValue(json, PlanGeneration.class);
            if (g.getPath() == null) g.setPath(fallbackPath);
            return g;
        } catch (Exception e) {
            log.warn("[planner] LLM JSON 解析失败，原文前 200 字: {}", raw == null ? "null" : raw.substring(0, Math.min(200, raw.length())));
            throw new BizException("AI 返回格式异常，建议重试");
        }
    }

    private String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int n = t.indexOf('\n');
            if (n > 0) t = t.substring(n + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }
        return t;
    }
}
