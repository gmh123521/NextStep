package com.nextstep.ai.service;

import com.nextstep.ai.client.ChatModels.Message;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.service.AnalysisService;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评分解读：拿评分 + 画像，让 LLM 用学长学姐口吻给一段个性化分析（流式）
 * 流式输出完成后写入 Redis 缓存（覆盖式，TTL 24h），下次进页面优先读缓存
 */
@Service
@RequiredArgsConstructor
public class ExplainService {

    private final DashScopeClient llm;
    private final AnalysisService analysisService;
    private final UserProfileService userProfileService;
    private final StringRedisTemplate redis;

    private static final String CACHE_KEY_PREFIX = "nextstep:ai:explain:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private static final String SYSTEM_PROMPT = """
            你是 NextStep 的资深升学就业咨询师，语气像比用户大 5 岁的学长学姐：直接、专业、不油腻。

            【输入】下面会给你一份用户画像 + 三条路径（考研/考公/就业）的评分结果。

            【输出要求】
            1) 中文 200-250 字，写两段（第一段优势+瓶颈，第二段下一步动作），中间空一行
            2) 第一段：先肯定 1-2 个真实优势，再点出最关键的 1 个瓶颈；如果分数普遍很高（>80），可以略过瓶颈，改说"建议如何更进一步"
            3) 第二段：给 1 个具体动作，要有可衡量结果（如"暑假投 5 家大厂实习"而不是"多做实习"）；动作要匹配画像里的"风险偏好"和"备考预算"，别让保守型用户去赌冲名校

            【硬性禁忌】
            - 不要重复原始分数（用户在卡片里看得见）
            - 不要使用"加油""你一定可以""相信自己"这类鸡汤话
            - 不要使用编号清单（1. 2. 3.），自然段落即可
            - 不要使用"咱们""哥们"等过度口语化称呼

            【缺数据处理】
            - 画像中明确为 null 的字段，不要假设它"缺乏"，可以略过；不要写"你似乎没有实习"这种推断
            - 若关键字段（如目标路径、GPA）缺失，明确建议"补全画像后再评估更准"

            【常识表（用于客观评估，不要照抄到回答里）】
            - CET-4 / CET-6 满分 710；425 及格、500 中等、550+ 较高、600+ 高分；<425 视为未通过
            - TEM-4 / TEM-8 满分 100；60 及格、80+ 良好、85+ 优秀
            - 雅思满分 9.0；6.0 一般、6.5 较好、7.0+ 优秀
            - 托福满分 120；80 一般、95 较好、105+ 优秀
            - JLPT N1 高级、N2 中高、N3 及以下偏初级；TOPIK Ⅱ 高级、TOPIK Ⅰ 中初级
            - 看到"未通过/远低于及格线"的语言分数，必须如实指出"语言短板需要补"，禁止美化为"高分""不错"等
            """;

    /** 读缓存（无则返回 null） */
    public String getCached(Long userId) {
        return redis.opsForValue().get(cacheKey(userId));
    }

    public Flux<String> explainStream(Long userId) {
        AnalysisResult result = analysisService.analyze(userId);
        UserProfile profile = userProfileService.get(userId);
        String userPrompt = buildUserPrompt(profile, result);

        List<Message> messages = List.of(
                new Message("system", SYSTEM_PROMPT),
                new Message("user", userPrompt)
        );

        StringBuilder accumulator = new StringBuilder();
        return llm.stream(messages)
                .doOnNext(accumulator::append)
                .doOnComplete(() -> redis.opsForValue()
                        .set(cacheKey(userId), accumulator.toString(), CACHE_TTL));
    }

    private String cacheKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }

    private String buildUserPrompt(UserProfile p, AnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("【画像】\n");
        appendIf(sb, "院校", p.getCurrentSchool());
        appendIf(sb, "院校层次", p.getSchoolLevel());
        appendIf(sb, "专业", p.getCurrentMajor());
        appendIf(sb, "学历", p.getDegreeType());
        if (p.getGpa() != null) sb.append("- GPA: ").append(p.getGpa())
                .append(" (满分 ").append(p.getGpaScale() == null ? 4 : p.getGpaScale()).append(")\n");
        appendIf(sb, "英语等级", p.getEnglishLevel());
        if (p.getEnglishScore() != null) {
            String level = describeEnglishScore(p.getEnglishLevel(), p.getEnglishScore());
            sb.append("- 英语分数: ").append(p.getEnglishScore());
            if (level != null) sb.append("（").append(level).append("）");
            sb.append("\n");
        }
        appendIf(sb, "目标路径", p.getTargetPaths());
        appendIf(sb, "偏好城市", p.getPreferredRegions());
        appendIf(sb, "当前状态", p.getCurrentStatus());
        if (p.getRiskAppetite() != null) sb.append("- 风险偏好(1-5): ").append(p.getRiskAppetite()).append("\n");
        if (p.getMonthlyBudget() != null) sb.append("- 月度备考预算: ").append(p.getMonthlyBudget()).append("元\n");

        sb.append("\n【评分结果】\n");
        sb.append("综合推荐主线: ").append(r.getTopPath() == null ? "暂无" : r.getTopPath()).append("\n");
        for (PathScore ps : r.getPaths()) {
            sb.append("- ").append(ps.getPathName()).append(" 综合 ").append(ps.getOverall()).append(" / 100");
            sb.append("（").append(ps.getDimensions().stream()
                    .map(d -> d.getName() + " " + d.getScore())
                    .collect(Collectors.joining("、"))).append("）\n");
        }
        return sb.toString();
    }

    private void appendIf(StringBuilder sb, String label, String v) {
        if (v != null && !v.isBlank()) sb.append("- ").append(label).append(": ").append(v).append("\n");
    }

    /** 把英语分数翻译成"评级"提示，避免 LLM 因不知道满分而乱评 */
    private String describeEnglishScore(String level, int score) {
        if (level == null) return null;
        return switch (level) {
            case "CET4", "CET6" -> {
                if (score < 425) yield "未通过及格线 425/710";
                if (score < 500) yield "及格 425-500/710";
                if (score < 550) yield "中等 500-550/710";
                if (score < 600) yield "较高 550-600/710";
                yield "高分 600+/710";
            }
            case "TEM4", "TEM8" -> {
                if (score < 60) yield "未通过 <60/100";
                if (score < 80) yield "良好 60-80/100";
                yield "优秀 80+/100";
            }
            case "IELTS" -> {
                // 用户输入的是 ×10 整数，60 = 6.0
                if (score < 50) yield "偏弱 <5.0/9";
                if (score < 65) yield "一般 5.0-6.5/9";
                if (score < 75) yield "良好 6.5-7.5/9";
                yield "优秀 7.5+/9";
            }
            case "TOEFL" -> {
                if (score < 80) yield "偏弱 <80/120";
                if (score < 95) yield "一般 80-95/120";
                if (score < 105) yield "较好 95-105/120";
                yield "优秀 105+/120";
            }
            default -> null;
        };
    }
}
