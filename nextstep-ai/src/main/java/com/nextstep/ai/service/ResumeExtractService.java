package com.nextstep.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.ai.client.ChatModels.Message;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.ai.dto.ResumeApplyResult;
import com.nextstep.ai.dto.ResumeExtractResult;
import com.nextstep.ai.entity.UserExperience;
import com.nextstep.ai.mapper.UserExperienceMapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 简历解析：PDF → 文本 → LLM 抽取结构化 JSON
 * 设计原则：
 *  - 抽取后只返回给前端展示，不直接覆盖画像
 *  - 用户确认后调用 apply() 才入库
 *  - 涉密字段（手机/邮箱/身份证）不抽取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractService {

    private final DashScopeClient llm;
    private final UserProfileService userProfileService;
    private final UserExperienceMapper experienceMapper;
    private final ExperienceSummaryService summaryService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeExperienceDeduplicator deduplicator = new ResumeExperienceDeduplicator();
    private final ResumeResultValidator resultValidator = new ResumeResultValidator();

    private static final int MAX_PDF_BYTES = 5 * 1024 * 1024;
    private static final int MAX_RESUME_CHARS = 8000;

    private static final String SYSTEM_PROMPT = """
            你是简历信息抽取助手。从用户提供的简历文本中抽取结构化字段，严格输出 JSON。

            规则：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块、不要任何解释文字
            2. 不要抽取个人联系方式（手机、邮箱、地址、身份证）
            3. 字段缺失时不要凭空编造，留 null 即可
            4. schoolLevel 枚举：C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE（依据院校公认层次判断）
            5. degreeType 枚举：BACHELOR/MASTER/DOCTOR
            6. englishLevel 枚举：CET4/CET6/TEM4/TEM8/IELTS/TOEFL/JLPT_N1..N5/TOPIK1/TOPIK2/OTHER
            7. gpaScale 只能是 4/5/100；如果简历只写"GPA 3.85"等，默认按 4 推断
            8. gradeYear 取值：本科 1-4，硕士 5-7，博士 5-7（根据当前年级或入学年份计算）
             9. experiences[].type 枚举：INTERNSHIP（实习）/PROJECT（项目）/AWARD（奖项）/RESEARCH（科研）/PAPER（论文）/COMPETITION（学科竞赛）
             10. experiences[].description 尽可能保留简历原文的完整描述，不要缩写或概括，单条最长 3000 字
             11. 日期统一格式：YYYY-MM 或 YYYY；不确定则留 null
             12. notes 数组里的内容必须是「纯中文自然语言」，写给最终用户看：
                 - 严禁出现任何英文字段名（如 gradeYear/englishLevel/schoolLevel 等），用中文术语代替（如「年级」「英语等级」「院校层次」）
                 - 严禁出现任何英文枚举值（如 REGULAR/BACHELOR/CET6/C9/MASTER 等），全部翻译成中文（如「普通本科」「本科」「英语六级」「C9 联盟」「硕士」）
                 - 最多 3 条，每条 30 字以内

            JSON 结构：
            {
              "currentSchool": "string|null",
              "schoolLevel": "string|null",
              "currentMajor": "string|null",
              "degreeType": "string|null",
              "gradeYear": "int|null",
              "gpa": "number|null",
              "gpaScale": "int|null",
              "englishLevel": "string|null",
              "englishScore": "int|null",
              "experiences": [
                {"type": "...", "title": "...", "role": "...", "startDate": "...", "endDate": "...", "description": "..."}
              ],
              "notes": ["如有不确定项，用纯中文简短说明"]
            }
            """;

    /** 解析 PDF + LLM 抽取（不入库） */
    public ResumeExtractResult parse(MultipartFile file) {
        if (file.isEmpty()) throw new BizException("简历文件为空");
        if (file.getSize() > MAX_PDF_BYTES) throw new BizException("简历文件过大（限 5MB）");
        log.info("[resume] 开始解析: name={}, size={}", file.getOriginalFilename(), file.getSize());

        String text = extractPdfText(file);
        if (text.isBlank()) throw new BizException("未从 PDF 中提取到文本，可能是扫描件，请上传文字版 PDF");
        if (text.length() > MAX_RESUME_CHARS) text = text.substring(0, MAX_RESUME_CHARS);
        log.info("[resume] 文本长度 {} 字符，调用 LLM 抽取", text.length());

        long start = System.currentTimeMillis();
        List<Message> messages = List.of(
                new Message("system", SYSTEM_PROMPT),
                new Message("user", "以下是简历文本，请抽取：\n\n" + text)
        );
        String json;
        try {
            json = llm.chat(messages);
        } catch (Exception e) {
            log.warn("[resume] LLM 首次调用失败: {}，重试一次", e.getMessage());
            try {
                json = llm.chat(messages);
            } catch (Exception e2) {
                log.error("[resume] LLM 重试仍失败: {}", e2.getMessage());
                throw new BizException("AI 抽取超时或失败，请稍后重试");
            }
        }
        log.info("[resume] LLM 返回，耗时 {}ms, 长度 {}",
                System.currentTimeMillis() - start, json == null ? 0 : json.length());

        return parseLlmJson(json);
    }

    /** 用户确认后入库：覆盖画像核心字段 + 追加经历（同类型同标题去重） */
    @Transactional
    public ResumeApplyResult apply(Long userId, ResumeExtractResult r) {
        resultValidator.validateAndNormalize(r);

        // 画像：复用已有的 upsert 逻辑（只覆盖非空字段）
        UserProfileRequest req = new UserProfileRequest();
        req.setCurrentSchool(r.getCurrentSchool());
        req.setSchoolLevel(r.getSchoolLevel());
        req.setCurrentMajor(r.getCurrentMajor());
        req.setDegreeType(r.getDegreeType());
        req.setGradeYear(r.getGradeYear());
        if (r.getGpa() != null) req.setGpa(java.math.BigDecimal.valueOf(r.getGpa()));
        req.setGpaScale(r.getGpaScale());
        req.setEnglishLevel(r.getEnglishLevel());
        req.setEnglishScore(r.getEnglishScore());
        userProfileService.upsert(userId, req);

        ResumeApplyResult applyResult = new ResumeApplyResult();
        if (r.getExperiences() == null || r.getExperiences().isEmpty()) return applyResult;

        // 已有经历按语义分组维护指纹和规范化标题，避免跨类型误判重复
        Set<String> existingFingerprints = new HashSet<>();
        Map<String, List<String>> existingTitlesByGroup = new HashMap<>();
        List<UserExperience> existing = experienceMapper.selectList(
                new LambdaQueryWrapper<UserExperience>().eq(UserExperience::getUserId, userId));
        for (UserExperience e : existing) {
            deduplicator.remember(existingFingerprints, existingTitlesByGroup, e.getType(), e.getTitle());
        }

        // 阶段 1：先过滤出真要入库的项（同步、廉价），避免给被跳过的项白白调 LLM 摘要
        java.util.List<ResumeExtractResult.ExperienceItem> toInsert = new java.util.ArrayList<>();
        for (ResumeExtractResult.ExperienceItem item : r.getExperiences()) {
            if (item.getTitle() == null || item.getTitle().isBlank()) continue;
            if (deduplicator.isDuplicate(existingFingerprints, existingTitlesByGroup,
                    item.getType(), item.getTitle())) {
                applyResult.setSkipped(applyResult.getSkipped() + 1);
                continue;
            }
            // 立即记住本批次已接受项，让后续近似标题也能被去重
            deduplicator.remember(existingFingerprints, existingTitlesByGroup,
                    item.getType(), item.getTitle());
            toInsert.add(item);
        }

        // 阶段 2：并发调 LLM 生成摘要（多条经历串行 ~60s → 并发 ~10s）
        // parallelStream 走的是 ForkJoinPool.commonPool，桌面服务器 CPU 数足够覆盖一份简历的几条经历
        java.util.Map<Integer, String> summaries = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.stream.IntStream.range(0, toInsert.size()).parallel().forEach(i -> {
            String s = summaryService.generate(toInsert.get(i).getDescription());
            if (s != null) summaries.put(i, s);
        });

        // 阶段 3：串行入库（保持事务 + 顺序）
        for (int i = 0; i < toInsert.size(); i++) {
            ResumeExtractResult.ExperienceItem item = toInsert.get(i);
            UserExperience exp = new UserExperience();
            exp.setUserId(userId);
            exp.setType(item.getType());
            exp.setTitle(item.getTitle());
            exp.setRole(item.getRole());
            exp.setStartDate(item.getStartDate());
            exp.setEndDate(item.getEndDate());
            exp.setDescription(item.getDescription());
            exp.setSummary(summaries.get(i));
            exp.setSource("RESUME");
            exp.setCreatedAt(LocalDateTime.now());
            exp.setUpdatedAt(LocalDateTime.now());
            experienceMapper.insert(exp);
            applyResult.setInserted(applyResult.getInserted() + 1);
        }

        // has_* 4 个标志位已改为派生字段（UserProfileService 从 experience 表实时聚合），
        // 这里不再手动 set，避免"标志位 1 但用户后来删了经历"的脏数据

        return applyResult;
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            log.error("PDF 解析失败: {}", e.getMessage());
            throw new BizException("PDF 解析失败：" + e.getMessage());
        }
    }

    private ResumeExtractResult parseLlmJson(String raw) {
        String json = stripCodeFence(raw);
        try {
            return objectMapper.readValue(json, ResumeExtractResult.class);
        } catch (Exception e) {
            log.warn("LLM JSON 解析失败，原文：{}", raw);
            throw new BizException("AI 返回格式异常，建议重试或检查 PDF 内容");
        }
    }

    /** 容错：去掉 ```json ... ``` 包裹 */
    private String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }
        return t;
    }
}
