package com.nextstep.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.ai.dto.TranscriptApplyResult;
import com.nextstep.ai.dto.TranscriptExtractResult;
import com.nextstep.ai.entity.UserCourse;
import com.nextstep.ai.mapper.UserCourseMapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 成绩单识别：图片/PDF → 多模态 LLM → 抽取课程列表 + 计算 GPA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptExtractService {

    private final DashScopeClient llm;
    private final UserCourseMapper courseMapper;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private static final String SYSTEM_PROMPT = """
            你是高校成绩单识别助手。从图片中识别每一门课程并按学校真实制式输出。

            【输出要求】
            1. 只输出一个 JSON 对象，禁止 Markdown 代码块、禁止解释文字
            2. 不要识别个人联系方式（手机/邮箱/身份证），但可以识别学号、姓名、学校、专业
            3. 缺失字段填 null，不要凭空编造

            ━━━━━━━━━━ GPA 处理铁律（最重要）━━━━━━━━━━
            4. **你必须只识别成绩单上的原始值，禁止做任何换算或制式猜测**
            5. computedGpa：成绩单上学校官方写的 GPA 数字，原样输出
               - 例：成绩单写"平均学分绩点 4.71/5.0" → computedGpa=4.71（**不是 3.77，不是任何换算值**）
               - 例：成绩单写"GPA: 3.85/4.0" → computedGpa=3.85
               - 例：成绩单写"加权平均分 88.5" → computedGpa=88.5
               - 如果成绩单没有官方汇总值，computedGpa 填 null（让后端自己算）
            6. gpaScale：必须从成绩单文字明示中识别，不是凭数字范围猜
               - 看到"满分 5.0"或"/5.0" → gpaScale=5
               - 看到"满分 4.0"或"/4.0" → gpaScale=4
               - 看到"加权平均分""百分制平均分" → gpaScale=100
               - **禁止"数字>4 所以猜 5 分制"这种主观推测**；找不到明示就填 null
            7. officialGpaText：把成绩单上写 GPA 那行**完整原文**抄过来作为审计证据
               - 例："平均学分绩点：4.71（满分 5.0）"
               - 例："GPA: 3.85 / 4.0"
               - 找不到就填 null
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            8. score：每门课的"原始成绩"（百分制 0-100，或等级转换）
               - 等级转换：优/A=92, 良/B=82, 中/C=72, 及格/D=62, 不及格/F=50
            9. course.gpa：每门课的"绩点"（4 分制，单门），按以下规则换算：
               90+ → 4.0, 85-89 → 3.7, 82-84 → 3.3, 78-81 → 3.0,
               75-77 → 2.7, 72-74 → 2.3, 68-71 → 2.0, 64-67 → 1.5, 60-63 → 1.0, <60 → 0
            10. semester 格式：YYYY-YYYY-N（如 2024-2025-1），无法判断时填 null
            11. notes 数组：纯中文，最多 3 条，禁止英文字段名/枚举值，每条 ≤30 字
                - 如果直接用了成绩单官方 GPA，写"使用了成绩单官方 GPA：X.XX"
                - 如果没找到官方 GPA，写"成绩单未提供官方 GPA"

            【JSON 结构】
            {
              "studentName": "string|null",
              "studentId": "string|null",
              "schoolName": "string|null",
              "majorName": "string|null",
              "computedGpa": "number|null",
              "gpaScale": 4 | 5 | 100 | null,
              "officialGpaText": "string|null",
              "totalCredit": "number|null",
              "courses": [
                {"courseName":"...","credit":3.0,"score":88,"gpa":3.7,"semester":"2024-2025-1","category":"必修"}
              ],
              "notes": []
            }
            """;

    public TranscriptExtractResult parse(MultipartFile file) {
        if (file.isEmpty()) throw new BizException("成绩单文件为空");
        if (file.getSize() > MAX_BYTES) throw new BizException("成绩单文件过大（限 8MB）");
        log.info("[transcript] 开始解析: name={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        byte[] imageBytes;
        String mime;
        try {
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            if (contentType.contains("pdf") || (file.getOriginalFilename() != null
                    && file.getOriginalFilename().toLowerCase().endsWith(".pdf"))) {
                imageBytes = pdfFirstPageToPng(file.getBytes());
                mime = "image/jpeg";
                log.info("[transcript] PDF 首页已转 JPEG，{} bytes", imageBytes.length);
            } else {
                imageBytes = file.getBytes();
                mime = contentType.startsWith("image/") ? contentType : "image/png";
            }
        } catch (IOException e) {
            log.error("成绩单读取失败: {}", e.getMessage());
            throw new BizException("成绩单读取失败：" + e.getMessage());
        }

        log.info("[transcript] 调用多模态 LLM ({}), base64 后约 {} KB",
                "vision", imageBytes.length * 4 / 3 / 1024);
        long start = System.currentTimeMillis();
        String json;
        try {
            json = llm.visionBytes(SYSTEM_PROMPT, imageBytes, mime);
        } catch (Exception e) {
            log.warn("[transcript] LLM 首次调用失败: {}，重试一次", e.getMessage());
            try {
                json = llm.visionBytes(SYSTEM_PROMPT, imageBytes, mime);
            } catch (Exception e2) {
                log.error("[transcript] LLM 重试仍失败: {}", e2.getMessage());
                throw new BizException("AI 识别超时或失败，请稍后重试");
            }
        }
        log.info("[transcript] LLM 返回，耗时 {}ms, 长度 {}",
                System.currentTimeMillis() - start, json == null ? 0 : json.length());

        TranscriptExtractResult r = parseLlmJson(json);

        // 后端兜底：只有当 LLM 没识别到官方 GPA 时，才用学分加权重算
        // 如果 LLM 给了 computedGpa（来自成绩单原文），保持不动
        if (r.getComputedGpa() == null) {
            Integer scale = r.getGpaScale();
            if (scale == null) scale = 100;
            BigDecimal recomputed = recomputeGpa(r, scale);
            if (recomputed != null) {
                r.setComputedGpa(recomputed);
                r.setGpaScale(scale);
            }
        } else if (r.getGpaScale() == null) {
            // 有 GPA 没标制式：按数值兜底（>10 百分制，4-10 五分制，<=4 四分制）
            double v = r.getComputedGpa().doubleValue();
            r.setGpaScale(v > 10 ? 100 : v > 4.5 ? 5 : 4);
        }
        return r;
    }

    @Transactional
    public TranscriptApplyResult apply(Long userId, TranscriptExtractResult r, boolean syncProfileGpa) {
        TranscriptApplyResult result = new TranscriptApplyResult();

        if (r.getCourses() == null || r.getCourses().isEmpty()) {
            // 即使没有课程也允许同步 GPA
            if (syncProfileGpa && r.getComputedGpa() != null) {
                updateProfileGpa(userId, r.getComputedGpa(), r.getGpaScale());
                result.setProfileGpaUpdated(true);
            }
            return result;
        }

        // 已有课程指纹：courseName + semester
        Set<String> existing = new HashSet<>();
        for (UserCourse c : courseMapper.selectList(
                new LambdaQueryWrapper<UserCourse>().eq(UserCourse::getUserId, userId))) {
            existing.add(courseFingerprint(c.getCourseName(), c.getSemester()));
        }

        Set<String> seenInBatch = new HashSet<>();
        for (TranscriptExtractResult.CourseItem item : r.getCourses()) {
            if (item.getCourseName() == null || item.getCourseName().isBlank()) continue;
            String fp = courseFingerprint(item.getCourseName(), item.getSemester());
            if (existing.contains(fp) || !seenInBatch.add(fp)) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            UserCourse c = new UserCourse();
            c.setUserId(userId);
            c.setCourseName(item.getCourseName());
            c.setCredit(item.getCredit());
            c.setScore(item.getScore());
            c.setGpa(item.getGpa());
            c.setSemester(item.getSemester());
            c.setCategory(item.getCategory());
            c.setSource("TRANSCRIPT");
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            courseMapper.insert(c);
            result.setInserted(result.getInserted() + 1);
        }

        if (syncProfileGpa && r.getComputedGpa() != null) {
            updateProfileGpa(userId, r.getComputedGpa(), r.getGpaScale());
            result.setProfileGpaUpdated(true);
        }
        return result;
    }

    private void updateProfileGpa(Long userId, BigDecimal gpa) {
        UserProfileRequest req = new UserProfileRequest();
        req.setGpa(gpa);
        // 不再写死 4，由调用方传入对应制式
        userProfileService.upsert(userId, req);
    }

    private void updateProfileGpa(Long userId, BigDecimal gpa, Integer scale) {
        UserProfileRequest req = new UserProfileRequest();
        req.setGpa(gpa);
        req.setGpaScale(scale == null ? 100 : scale);
        userProfileService.upsert(userId, req);
    }

    /**
     * 按学校真实制式重算 GPA：
     *   - 4 分制 / 5 分制：用 course.gpa 学分加权（gpa 已是 4 分；5 分制需放大 1.25）
     *   - 100 分制：用 course.score 学分加权得到百分制平均分
     */
    private BigDecimal recomputeGpa(TranscriptExtractResult r, int scale) {
        if (r.getCourses() == null || r.getCourses().isEmpty()) return null;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;
        for (TranscriptExtractResult.CourseItem c : r.getCourses()) {
            if (c.getCredit() == null) continue;
            BigDecimal val;
            if (scale == 100) {
                if (c.getScore() == null) continue;
                val = c.getScore();
            } else {
                if (c.getGpa() == null) continue;
                // 5 分制 = 4 分制 × 1.25
                val = scale == 5 ? c.getGpa().multiply(BigDecimal.valueOf(1.25)) : c.getGpa();
            }
            weightedSum = weightedSum.add(val.multiply(c.getCredit()));
            creditSum = creditSum.add(c.getCredit());
        }
        if (creditSum.signum() == 0) return null;
        return weightedSum.divide(creditSum, 2, RoundingMode.HALF_UP);
    }

    private String courseFingerprint(String name, String semester) {
        String n = name == null ? "" : name.toLowerCase()
                .replaceAll("[\\s\\-_·\\.,()\\[\\]【】（）/／、，。]+", "");
        String s = semester == null ? "" : semester;
        return n + "|" + s;
    }

    private TranscriptExtractResult parseLlmJson(String raw) {
        String json = stripCodeFence(raw);
        try {
            return objectMapper.readValue(json, TranscriptExtractResult.class);
        } catch (Exception e) {
            log.warn("LLM JSON 解析失败，原文：{}", raw);
            throw new BizException("AI 返回格式异常，建议重试或换张更清晰的图片");
        }
    }

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

    /** PDF 首页转 PNG（100 DPI 即可识别清晰文字，且 base64 体积比 150 DPI 小一半） */
    private byte[] pdfFirstPageToPng(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.getNumberOfPages() == 0) throw new BizException("PDF 无页内容");
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage img = renderer.renderImageWithDPI(0, 100);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                // 用 JPEG 而非 PNG：成绩单文字 + 表格 JPEG 压缩损失可忽略，但体积小 60%+
                ImageIO.write(img, "jpg", out);
                return out.toByteArray();
            }
        }
    }
}
