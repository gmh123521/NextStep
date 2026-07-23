package com.nextstep.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.ai.client.ChatModels.Message;
import com.nextstep.ai.client.DashScopeClient;
import com.nextstep.ai.entity.UserExperience;
import com.nextstep.ai.mapper.UserExperienceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceSummaryService {

    private final DashScopeClient llm;
    private final UserExperienceMapper experienceMapper;

    private static final String SYSTEM_PROMPT = """
            你是简历经历摘要助手。把用户提供的一条经历（实习/项目/奖项/科研等）压缩成 ≤80 字的中文摘要。
            要求：
            1. 突出技术栈、解决的问题、量化成果，删掉客套话
            2. 一段话，不要列表
            3. 严禁编造原文没有的内容
            4. 直接输出摘要文字，不要任何前缀
            """;

    private static final int SUMMARY_SKIP_THRESHOLD = 80;

    public String generate(String description) {
        if (description == null || description.isBlank()) return null;
        if (description.length() <= SUMMARY_SKIP_THRESHOLD) return description;
        try {
            String summary = llm.chat(List.of(
                    new Message("system", SYSTEM_PROMPT),
                    new Message("user", description)
            ));
            return (summary == null || summary.isBlank())
                    ? truncate(description, 200)
                    : summary.trim();
        } catch (Exception e) {
            log.warn("[summary] LLM 摘要失败，降级为截断: {}", e.getMessage());
            return truncate(description, 200);
        }
    }

    public int backfillForUser(Long userId, boolean onlyMissing) {
        LambdaQueryWrapper<UserExperience> w = new LambdaQueryWrapper<UserExperience>()
                .eq(UserExperience::getUserId, userId);
        List<UserExperience> all = experienceMapper.selectList(w);
        int updated = 0;
        for (UserExperience e : all) {
            if (onlyMissing && e.getSummary() != null && !e.getSummary().isBlank()) continue;
            if (e.getDescription() == null || e.getDescription().isBlank()) continue;
            String s = generate(e.getDescription());
            if (s == null || s.equals(e.getSummary())) continue;
            e.setSummary(s);
            experienceMapper.updateById(e);
            updated++;
        }
        return updated;
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        int cut = maxLen;
        for (char sep : new char[]{'。', '；', '，', '、', ' '}) {
            int pos = text.lastIndexOf(sep, maxLen);
            if (pos > maxLen / 2) { cut = pos + 1; break; }
        }
        return text.substring(0, cut);
    }
}
