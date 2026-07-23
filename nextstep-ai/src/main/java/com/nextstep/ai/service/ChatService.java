package com.nextstep.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.ai.client.ChatModels;
import com.nextstep.ai.client.ChatModels.Message;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.ai.entity.UserExperience;
import com.nextstep.ai.mapper.UserExperienceMapper;
import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.analysis.service.AnalysisService;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对话式画像补缺
 *  - LLM 看用户画像缺什么，主动追问
 *  - LLM 决定调用 writeProfile / addExperience 时，后端真去改数据库
 *  - 上下文存 Redis（每个 userId 一个会话，TTL 1 小时）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final DashScopeClient llm;
    private final UserProfileService userProfileService;
    private final UserExperienceMapper experienceMapper;
    private final AnalysisService analysisService;
    private final ExperienceSummaryService summaryService;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    private static final String CACHE_KEY_PREFIX = "nextstep:ai:chat:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int MAX_HISTORY_MSGS = 30;     // 防止上下文爆炸
    private static final int MAX_TOOL_LOOPS = 4;        // 单次回合最多串几次工具调用

    private static final String SYSTEM_PROMPT = """
            你是用户的"学长"——一个已经上岸 2 年、走过考研/考公/求职三条路的过来人。
            你的目标是和用户聊天，**顺便**把"我的画像"里缺的字段聊出来，不是机械填表。

            【人设要点】
            - 真诚直接，有自己的判断（用户说不切实际的目标，要善意泼冷水）
            - 有共情（用户说累、焦虑、迷茫时，先回应情绪，再聊下一步）
            - 有经验视角（聊到考研用"这学校往年报录比怎样"，聊到就业用"这岗位面试一般考啥"）
            - 不油腻不鸡汤（避免"加油""你一定可以""相信自己"）
            - 不"老弟""哥们"（除非用户先这么说）

            【对话节奏】
            - 每次回复尽量两层：①回应用户上一句（共情、点评、分享一句经验）②自然引出下一个话题
            - 一次最多问一个问题（**严禁连续问 2 个以上**）
            - 用户开心闲聊就陪聊，**不要每条都强行追问字段**
            - 短句优先，不超过 80 字一段
            - **如果系统给了"三路径评分"快照，回复中要直接引用具体分数和短板维度**（如"考研 70 分，时间投入 75 分还行，但经济负担只有 45"），不要泛泛而谈
            - **绝不要问用户已经在画像里的事**（如已有实习经历就别问"实习项目多吗"，已有 GPA 就别问"成绩怎么样"）

            【后台抽取（用户感知不到）】
            用户的每条事实性表述都要落到工具调用，但**回复不要告诉用户"已记下"**（让操作隐形）：
            - 提到学校/专业/GPA/英语 → 调 writeProfile
            - 提到实习/项目/奖项/科研/论文/竞赛 → 调 addExperience
            - 提到兴趣/方向偏好 → writeProfile 的 interests
            - 提到优势（"我算法好"）→ writeProfile 的 strengths
            - 提到短板/吃力的事（"我政治弱""背政治脑袋疼""英语口语差"）→ writeProfile 的 weaknesses
            - 提到目标路径偏好 → writeProfile 的 targetPaths
            - 提到偏好城市 → writeProfile 的 preferredRegions
            - interests/strengths/weaknesses 是累加字段：每次只传"用户当前这条新增内容"，后端会自动合并
            - **关键**：用户即使是抱怨"XX 好难/好累/记不住"，本质上也是在透露短板，必须调 writeProfile 写 weaknesses（用一句话提炼，如"政治科目背诵吃力"），同时还是要做共情回复

            【抽取触发的具体例子（务必照做）】
            示例 1 — 用户："我政治比较弱，到底是考研还是就业"
                  → 必须调用 writeProfile({weaknesses: "政治薄弱"})，回复也照常做
            示例 2 — 用户："数学是我的强项"
                  → 必须调用 writeProfile({strengths: "数学功底好"})
            示例 3 — 用户："想去北京"
                  → 必须调用 writeProfile({preferredRegions: "北京"})
            示例 4 — 用户："去年实习过字节"
                  → 必须调用 addExperience({type:"INTERNSHIP", title:"字节跳动"})
            示例 5 — 用户："感觉考研好难"（没具体说哪一科）
                  → 这种过于笼统的抱怨**不要**强行抽 weaknesses，只共情回复即可
            原则：用户陈述了具体短板/优势/兴趣/经历，**事实落库**；只是抒情/提问/闲聊，**不强行抽取**

            【字段值规则】
            - schoolLevel：C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE（用户说"双一流"→DOUBLE_FIRST）
            - degreeType：BACHELOR/MASTER/DOCTOR
            - targetPaths：考研→PG，考公→CS，就业→EM，多个用逗号分隔
            - currentStatus：IN_SCHOOL/PREPARING/JOB_HUNTING/GRADUATED/EMPLOYED

            【语言分数常识（必须记住，不要照抄到对话里）】
            - CET-4 / CET-6 满分 710；425 及格、500 中等、550+ 较高、600+ 高分；**<425 视为未通过**
            - TEM-4 / TEM-8 满分 100；60 及格、80+ 良好、85+ 优秀
            - 雅思满分 9.0；6.0 一般、6.5 较好、7.0+ 优秀
            - 托福满分 120；80 一般、95 较好、105+ 优秀
            - 看到"未通过/远低于及格线"的分数，必须如实说"这个分数偏低"或"可能需要补一下"
            - **禁止把 <425 的 CET 分数称为"不错""不低""中等"，必须指出"未过线"的事实**

            【硬性禁忌】
            - **严禁编造具体数字/年份/学校经历**（不要说"我当年考了 380 分"，可以说"我那届分数线大概 350"）
            - **严禁只回话不调工具**（聊天可以发散，但事实落库不能省）
            - **严禁说"已记下""已为你保存"**（用户感知不到操作）
            - 严禁使用编号清单 1. 2. 3.
            - 严禁重复用户已经填好的字段
            - 严禁同一句话同时问 2 个问题
            """;

    /** 用户消息 → AI 回复（同步返回，含本回合工具调用结果） */
    public ChatTurnResult chat(Long userId, String userMessage) {
        return chatInternal(userId, userMessage, false);
    }

    /** 开场白：让 AI 看一眼画像主动开口，不计入历史，不允许调工具 */
    public ChatTurnResult kickoff(Long userId) {
        UserProfile profile = userProfileService.get(userId);
        String snapshot = renderProfileSnapshot(profile, userId);
        String prompt = """
                请用 1-2 句话开场（共 ≤60 字），不要自我介绍，按下面规则三选一：
                A. 画像几乎是空的（已填字段 < 3）：先打个招呼再问"你现在哪个学校读什么专业？"
                B. 画像已有部分关键字段（3-7 个）：根据已知信息说一句感同身受/点评，再问最关键的缺失字段
                   例：用户已有"清华 + 计算机 + 大三"，开场可以说"清华计科大三啊，已经在准备保研还是考研了？"
                C. 画像看着挺全（≥8 个字段）：用学长口吻问"想聊点啥？保研、就业还是别的？我都聊得来"
                节奏：克制不油腻，避免"加油""你一定可以"等鸡汤
                """;

        List<Message> messages = List.of(
                new Message("system", SYSTEM_PROMPT),
                new Message("system", "【当前画像】\n" + snapshot),
                new Message("user", prompt)
        );
        ChatModels.Choice choice = llm.chatWithTools(messages, null);
        ChatTurnResult result = new ChatTurnResult();
        result.setReply(extractText(choice.getMessage().getContent()));
        // 把开场白存入历史，但不存触发用的"system 提示"
        ChatHistory history = loadHistory(userId);
        history.append(simpleAssistantMsg(result.getReply()));
        history.trim(MAX_HISTORY_MSGS);
        saveHistory(userId, history);
        return result;
    }

    private ChatTurnResult chatInternal(Long userId, String userMessage, boolean isKickoff) {
        ChatHistory history = loadHistory(userId);

        // 把"当前画像"作为系统消息一同送进去（每次刷新最新值）
        UserProfile profile = userProfileService.get(userId);
        String profileSnapshot = renderProfileSnapshot(profile, userId);

        // 注入三路径评分（让学长看着分数说话，不再泛泛而谈）
        String scoreSnapshot = renderScoreSnapshot(userId);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", SYSTEM_PROMPT));
        messages.add(new Message("system", "【当前画像】\n" + profileSnapshot));

        // 注入"详细经历列表"——让 LLM 能直接说出项目名、引用摘要，不再反问"项目叫啥"
        String expDetail = renderExperienceSnapshot(userId, true);
        if (!expDetail.isEmpty()) {
            messages.add(new Message("system",
                    "【经历详情（学长已知道这些，不要再反问名称/内容，可直接评点）】\n" + expDetail));
        }

        if (scoreSnapshot != null) {
            messages.add(new Message("system", "【三路径评分（请直接引用具体分数和短板维度）】\n" + scoreSnapshot));
        }
        // 工程兜底：从用户消息中扫描"事实关键词"，命中后给 LLM 一条强提示
        String hint = buildToolHint(userMessage);
        if (hint != null) {
            messages.add(new Message("system", hint));
        }
        // 按需注入经历详情：只在用户聊到经历相关话题时才附上摘要，日常闲聊不加
        if (matchExpKeywords(userMessage)) {
            String expDetails = renderExperienceSnapshot(userId, true);
            if (!expDetails.isEmpty()) {
                messages.add(new Message("system", "【经历详情（本次对话涉及经历话题，附上摘要供你引用）】\n" + expDetails));
            }
        }
        messages.addAll(history.getMessages());
        messages.add(new Message("user", userMessage));

        ChatTurnResult result = new ChatTurnResult();

        // tool 循环：LLM 可能连续调用工具直到给出最终回复
        int loop = 0;
        Message assistantMsg = null;
        while (loop++ < MAX_TOOL_LOOPS) {
            ChatModels.Choice choice = llm.chatWithTools(messages, ChatToolRegistry.tools());
            assistantMsg = choice.getMessage();
            messages.add(assistantMsg);

            List<ChatModels.ToolCall> calls = assistantMsg.getTool_calls();
            if (calls == null || calls.isEmpty()) break;

            // 执行每个工具调用，结果作为 tool 消息追加进上下文
            for (ChatModels.ToolCall call : calls) {
                String toolResult = dispatchTool(userId, call, result);
                Message toolMsg = new Message();
                toolMsg.setRole("tool");
                toolMsg.setTool_call_id(call.getId());
                toolMsg.setName(call.getFunction().getName());
                toolMsg.setContent(toolResult);
                messages.add(toolMsg);
            }
        }

        String reply = assistantMsg == null ? "" : extractText(assistantMsg.getContent());
        result.setReply(reply);

        // 持久化（只存"对外可见"的对话，工具消息不存）
        history.append(new Message("user", userMessage));
        history.append(simpleAssistantMsg(reply));
        history.trim(MAX_HISTORY_MSGS);
        saveHistory(userId, history);

        return result;
    }

    public void resetHistory(Long userId) {
        redis.delete(cacheKey(userId));
    }

    public List<Message> getHistoryForView(Long userId) {
        return loadHistory(userId).getMessages();
    }

    // ───── 工具分发 ─────

    private String dispatchTool(Long userId, ChatModels.ToolCall call, ChatTurnResult result) {
        String name = call.getFunction().getName();
        String argsJson = call.getFunction().getArguments();
        try {
            Map<String, Object> args = om.readValue(argsJson, new TypeReference<>() {});
            return switch (name) {
                case ChatToolRegistry.TOOL_WRITE_PROFILE  -> doWriteProfile(userId, args, result);
                case ChatToolRegistry.TOOL_ADD_EXPERIENCE -> doAddExperience(userId, args, result);
                default -> "{\"ok\":false,\"error\":\"未知工具：" + name + "\"}";
            };
        } catch (Exception e) {
            log.error("[chat tool] {} 执行失败: {}", name, e.getMessage());
            return "{\"ok\":false,\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String doWriteProfile(Long userId, Map<String, Object> args, ChatTurnResult result) {
        UserProfileRequest req = toRequest(args);
        // gpa 可能是 number → BigDecimal
        if (args.get("gpa") != null) {
            req.setGpa(new BigDecimal(args.get("gpa").toString()));
        }
        // 累加字段：interests / strengths / weaknesses
        // 把 LLM 给的"新增内容"和数据库里现有的旧值合并，用「；」分隔
        UserProfile current = userProfileService.get(userId);
        if (current != null) {
            req.setInterests(mergeAppendable(current.getInterests(), req.getInterests()));
            req.setStrengths(mergeAppendable(current.getStrengths(), req.getStrengths()));
            req.setWeaknesses(mergeAppendable(current.getWeaknesses(), req.getWeaknesses()));
        }
        userProfileService.upsert(userId, req);
        result.setProfileUpdated(true);
        result.getUpdatedFields().addAll(args.keySet());
        return "{\"ok\":true,\"updatedFields\":" + args.keySet().size() + "}";
    }

    /**
     * 累加字段合并：
     *  - 新值为 null/空 → 保留旧值不动（返回 null 表示 upsert 不要写）
     *  - 旧值为空 → 用新值
     *  - 旧值已经包含新值的全部内容 → 跳过（避免重复）
     *  - 否则旧值后追加「；新值」
     */
    private String mergeAppendable(String oldVal, String newVal) {
        if (newVal == null || newVal.isBlank()) return null;
        String n = newVal.trim();
        if (oldVal == null || oldVal.isBlank()) return n;
        String o = oldVal.trim();
        // 已经包含就跳过
        if (o.contains(n)) return null;
        // 反过来：新值已包含旧值的所有内容 → 用新值
        if (n.contains(o)) return n;
        return o + "；" + n;
    }

    private String doAddExperience(Long userId, Map<String, Object> args, ChatTurnResult result) {
        UserExperience exp = new UserExperience();
        exp.setUserId(userId);
        exp.setType(strOf(args.get("type")));
        exp.setTitle(strOf(args.get("title")));
        exp.setRole(strOf(args.get("role")));
        exp.setStartDate(strOf(args.get("startDate")));
        exp.setEndDate(strOf(args.get("endDate")));
        exp.setDescription(strOf(args.get("description")));
        exp.setSummary(summaryService.generate(exp.getDescription()));
        exp.setSource("CHAT");
        exp.setCreatedAt(LocalDateTime.now());
        exp.setUpdatedAt(LocalDateTime.now());
        experienceMapper.insert(exp);
        result.setExperienceAdded(true);
        return "{\"ok\":true,\"id\":" + exp.getId() + "}";
    }

    /** Map → UserProfileRequest 转换 */
    private UserProfileRequest toRequest(Map<String, Object> args) {
        UserProfileRequest req = new UserProfileRequest();
        if (args.containsKey("currentSchool"))      req.setCurrentSchool(strOf(args.get("currentSchool")));
        if (args.containsKey("schoolLevel"))        req.setSchoolLevel(strOf(args.get("schoolLevel")));
        if (args.containsKey("currentMajor"))       req.setCurrentMajor(strOf(args.get("currentMajor")));
        if (args.containsKey("majorCategory"))      req.setMajorCategory(strOf(args.get("majorCategory")));
        if (args.containsKey("degreeType"))         req.setDegreeType(strOf(args.get("degreeType")));
        if (args.containsKey("gradeYear"))          req.setGradeYear(intOf(args.get("gradeYear")));
        if (args.containsKey("gpaScale"))           req.setGpaScale(intOf(args.get("gpaScale")));
        if (args.containsKey("classRankPct") && args.get("classRankPct") != null)
                                                    req.setClassRankPct(new BigDecimal(args.get("classRankPct").toString()));
        if (args.containsKey("englishLevel"))       req.setEnglishLevel(strOf(args.get("englishLevel")));
        if (args.containsKey("englishScore"))       req.setEnglishScore(intOf(args.get("englishScore")));
        if (args.containsKey("targetPaths"))        req.setTargetPaths(strOf(args.get("targetPaths")));
        if (args.containsKey("preferredRegions"))   req.setPreferredRegions(strOf(args.get("preferredRegions")));
        if (args.containsKey("preferredIndustries"))req.setPreferredIndustries(strOf(args.get("preferredIndustries")));
        if (args.containsKey("salaryExpectation"))  req.setSalaryExpectation(intOf(args.get("salaryExpectation")));
        if (args.containsKey("riskAppetite"))       req.setRiskAppetite(intOf(args.get("riskAppetite")));
        if (args.containsKey("monthlyBudget"))      req.setMonthlyBudget(intOf(args.get("monthlyBudget")));
        if (args.containsKey("currentStatus"))      req.setCurrentStatus(strOf(args.get("currentStatus")));
        if (args.containsKey("interests"))          req.setInterests(strOf(args.get("interests")));
        if (args.containsKey("strengths"))          req.setStrengths(strOf(args.get("strengths")));
        if (args.containsKey("weaknesses"))         req.setWeaknesses(strOf(args.get("weaknesses")));
        return req;
    }

    private String strOf(Object o) { return o == null ? null : o.toString(); }
    private Integer intOf(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    // ───── 历史会话存取 ─────

    private ChatHistory loadHistory(Long userId) {
        String json = redis.opsForValue().get(cacheKey(userId));
        if (json == null || json.isBlank()) return new ChatHistory();
        try {
            List<Message> msgs = om.readValue(json, new TypeReference<>() {});
            ChatHistory h = new ChatHistory();
            h.setMessages(msgs);
            return h;
        } catch (Exception e) {
            return new ChatHistory();
        }
    }

    private void saveHistory(Long userId, ChatHistory h) {
        try {
            String json = om.writeValueAsString(h.getMessages());
            redis.opsForValue().set(cacheKey(userId), json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("save chat history failed: {}", e.getMessage());
        }
    }

    private String cacheKey(Long userId) { return CACHE_KEY_PREFIX + userId; }

    // ───── 辅助：构造对外可见的 assistant 消息（去掉 tool_calls） ─────

    private Message simpleAssistantMsg(String text) {
        Message m = new Message();
        m.setRole("assistant");
        m.setContent(text == null ? "" : text);
        return m;
    }

    private String extractText(Object content) {
        return content == null ? "" : content.toString();
    }

    /** 渲染当前画像快照供 system prompt 用 */
    private String renderProfileSnapshot(UserProfile p, Long userId) {
        if (p == null) return "（用户还没有任何画像数据，可以从「你目前在哪所学校读什么专业？」开始）";
        StringBuilder sb = new StringBuilder();
        appendIf(sb, "院校",        p.getCurrentSchool());
        appendIf(sb, "院校层次",    p.getSchoolLevel());
        appendIf(sb, "专业",        p.getCurrentMajor());
        appendIf(sb, "学科门类",    p.getMajorCategory());
        appendIf(sb, "学历",        p.getDegreeType());
        if (p.getGradeYear() != null)         sb.append("- 年级: ").append(p.getGradeYear()).append("\n");
        if (p.getGpa() != null)               sb.append("- GPA: ").append(p.getGpa()).append(" / ").append(p.getGpaScale() == null ? 4 : p.getGpaScale()).append("\n");
        if (p.getClassRankPct() != null)      sb.append("- 班级排名百分位: 前 ").append(p.getClassRankPct()).append("%\n");
        appendIf(sb, "语言等级",    p.getEnglishLevel());
        if (p.getEnglishScore() != null)      sb.append("- 语言分数: ").append(p.getEnglishScore()).append("\n");
        appendIf(sb, "目标路径",    p.getTargetPaths());
        appendIf(sb, "偏好城市",    p.getPreferredRegions());
        appendIf(sb, "偏好行业",    p.getPreferredIndustries());
        if (p.getSalaryExpectation() != null) sb.append("- 期望月薪: ").append(p.getSalaryExpectation()).append("\n");
        if (p.getRiskAppetite() != null)      sb.append("- 风险偏好: ").append(p.getRiskAppetite()).append("\n");
        if (p.getMonthlyBudget() != null)     sb.append("- 月度备考预算: ").append(p.getMonthlyBudget()).append("\n");
        appendIf(sb, "当前状态",    p.getCurrentStatus());
        appendIf(sb, "兴趣",        p.getInterests());
        appendIf(sb, "优势",        p.getStrengths());
        appendIf(sb, "短板",        p.getWeaknesses());

        String expIndex = renderExperienceSnapshot(userId, false);
        if (!expIndex.isEmpty()) {
            sb.append("\n【已有经历（标题目录，如需详情会另外提供）】\n");
            sb.append(expIndex);
        }

        if (sb.length() == 0) return "（画像还是空的）";
        return sb.toString();
    }

    /** 渲染经历列表，detailed=false 只出标题目录，detailed=true 附带摘要 */
    String renderExperienceSnapshot(Long userId, boolean detailed) {
        List<UserExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<UserExperience>()
                        .eq(UserExperience::getUserId, userId)
                        .orderByDesc(UserExperience::getEndDate)
                        .orderByDesc(UserExperience::getId));
        if (experiences == null || experiences.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int totalChars = 0;
        int maxTotal = detailed ? 3000 : 600;

        for (UserExperience exp : experiences) {
            String typeLabel = switch (exp.getType()) {
                case "INTERNSHIP" -> "实习";
                case "PROJECT" -> "项目";
                case "AWARD" -> "奖项";
                case "RESEARCH" -> "科研";
                case "PAPER" -> "论文";
                case "COMPETITION" -> "竞赛";
                default -> exp.getType();
            };

            StringBuilder line = new StringBuilder();
            line.append("  - [").append(typeLabel).append("] ").append(exp.getTitle());
            if (exp.getRole() != null && !exp.getRole().isBlank()) {
                line.append(" · ").append(exp.getRole());
            }
            if (exp.getStartDate() != null) {
                line.append(" (").append(exp.getStartDate());
                if (exp.getEndDate() != null) line.append(" ~ ").append(exp.getEndDate());
                line.append(")");
            }
            line.append("\n");

            if (detailed) {
                String detail = exp.getSummary();
                if (detail == null || detail.isBlank()) {
                    detail = truncateText(exp.getDescription(), 200);
                }
                if (detail != null && !detail.isBlank()) {
                    line.append("    ").append(truncateText(detail, 200)).append("\n");
                }
            }

            if (totalChars + line.length() > maxTotal) break;
            sb.append(line);
            totalChars += line.length();
        }

        return sb.toString();
    }

    /** 截取文本到指定长度，优先在句号处断开 */
    String truncateText(String text, int maxLen) {
        if (text == null || text.isBlank()) return null;
        if (text.length() <= maxLen) return text;
        int cut = maxLen;
        for (char sep : new char[]{'。', '；', '，', '、', ' '}) {
            int pos = text.lastIndexOf(sep, maxLen);
            if (pos > maxLen / 2) { cut = pos + 1; break; }
        }
        return text.substring(0, cut);
    }

    /** 三路径评分快照 — 让 LLM 看着分数说话 */
    private String renderScoreSnapshot(Long userId) {
        try {
            AnalysisResult r = analysisService.analyze(userId);
            if (r == null || r.getPaths() == null || r.getPaths().isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            if (r.getTopPath() != null) {
                sb.append("推荐主线: ").append(r.getTopPath());
                if (r.getTopPathReason() != null) sb.append("（").append(r.getTopPathReason()).append("）");
                sb.append("\n");
            }
            for (PathScore ps : r.getPaths()) {
                sb.append("- ").append(ps.getPathName()).append(" ").append(ps.getOverall()).append(" / 100");
                if (ps.getDimensions() != null && !ps.getDimensions().isEmpty()) {
                    sb.append("（");
                    sb.append(ps.getDimensions().stream()
                            .map(d -> d.getName() + " " + d.getScore())
                            .reduce((a, b) -> a + "、" + b).orElse(""));
                    sb.append("）");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;  // 画像不全时 analyze 会抛业务异常，对话场景静默兜底
        }
    }

    private void appendIf(StringBuilder sb, String label, String v) {
        if (v != null && !v.isBlank()) sb.append("- ").append(label).append(": ").append(v).append("\n");
    }

    // ───── 内部数据类 ─────

    @Data
    public static class ChatTurnResult {
        private String reply;
        private boolean profileUpdated;
        private boolean experienceAdded;
        private List<String> updatedFields = new ArrayList<>();
    }

    @Data
    public static class ChatHistory implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private List<Message> messages = new ArrayList<>();
        public void append(Message m) { messages.add(m); }
        public void trim(int max) {
            if (messages.size() > max) messages = new ArrayList<>(messages.subList(messages.size() - max, messages.size()));
        }
    }

    // ───────────────── 工程兜底：用户消息关键词扫描 → 强制工具调用提示 ─────────────────

    /**
     * 扫描用户消息里的"事实陈述"关键词，命中后给 LLM 一条 system 强提示，
     * 让它**这一轮一定调工具**，提升落库率。
     * LLM 偶尔会偷懒只闲聊不调工具——这一层防御能补上大部分漏抽。
     */
    private String buildToolHint(String msg) {
        if (msg == null) return null;
        String m = msg.toLowerCase();
        List<String> hints = new ArrayList<>();

        if (containsAny(m, "弱", "差", "不好", "拉胯", "拖后腿", "短板", "记不住", "学不会", "考不过", "挂科")) {
            hints.add("用户透露了短板：本轮**必须**调 writeProfile 传 weaknesses（提炼 1 句话）");
        }
        if (containsAny(m, "擅长", "强项", "拿手", "做得好", "底子好", "学得快")) {
            hints.add("用户透露了优势：本轮**必须**调 writeProfile 传 strengths");
        }
        if (containsAny(m, "感兴趣", "想做", "喜欢", "热爱", "向往", "心仪")) {
            hints.add("用户透露了兴趣：本轮**必须**调 writeProfile 传 interests");
        }
        if (containsAny(m, "实习", "项目", "比赛", "竞赛", "论文", "发表", "获奖", "拿过")) {
            hints.add("用户提到了经历：如能确定具体名称，**必须**调 addExperience");
        }
        if (containsAny(m, "想去", "留在", "回到", "去北京", "去上海", "去深圳", "去广州", "去杭州")) {
            hints.add("用户透露了城市偏好：本轮**必须**调 writeProfile 传 preferredRegions");
        }

        if (hints.isEmpty()) return null;
        return "【本轮抽取提示】\n" + String.join("\n", hints) + "\n仍然要做学长式共情回复，但工具调用不可缺。";
    }

    /** 扫用户消息里的经历相关词 → 是否需要注入经历详情 */
    boolean matchExpKeywords(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return containsAny(m,
                "实习", "项目", "经历", "做过", "简历", "比赛", "竞赛",
                "论文", "发表", "获奖", "科研", "奖项", "项目经历",
                "字节", "腾讯", "阿里", "百度", "美团", "京东", "网易",
                "以前", "之前", "那时候", "上个", "去年", "前年");
    }

    private boolean containsAny(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }
}
