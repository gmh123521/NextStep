package com.nextstep.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 ExperienceSummaryService 的纯逻辑部分：
 *  - generate() 对 null / 空 / 短文本的处理（不打 LLM）
 *  - truncate() 在各种分隔符场景下的断点选择
 *
 * 长文本走 LLM 的路径需要打桩 DashScopeClient，Java 23 暂时不引入 Mockito，
 * 这里跳过；集成验证由"调回填接口 → 查库"步骤覆盖。
 */
class ExperienceSummaryServiceTest {

    private final ExperienceSummaryService service = new ExperienceSummaryService(null, null);

    @Test
    @DisplayName("generate: null / 空白返回 null，不打 LLM")
    void generate_nullOrBlank() {
        assertNull(service.generate(null));
        assertNull(service.generate(""));
        assertNull(service.generate("   "));
    }

    @Test
    @DisplayName("generate: 短文本（≤80 字）原样返回，不打 LLM")
    void generate_shortText() {
        String shortText = "基于 Spring Boot 的内部 CRM，做了订单模块的查询性能优化";
        assertEquals(shortText, service.generate(shortText));
    }

    @Test
    @DisplayName("generate: 边界 — 正好 80 字返回原文")
    void generate_exact80() {
        String text = "a".repeat(80);
        assertEquals(text, service.generate(text));
    }

    @Test
    @DisplayName("truncate: 短文本原样返回")
    void truncate_short() throws Exception {
        assertEquals("Hello", invokeTruncate("Hello", 200));
    }

    @Test
    @DisplayName("truncate: 在句号处断开（位置需 > maxLen/2）")
    void truncate_sentenceBreak() throws Exception {
        assertEquals("第一句完整。", invokeTruncate("第一句完整。第二句被截掉", 8));
    }

    @Test
    @DisplayName("truncate: 句号过早，降级到逗号")
    void truncate_commaBreak() throws Exception {
        String result = invokeTruncate("很长很长很长的一句话，在这里有个逗号然后是更多内容", 12);
        assertEquals("很长很长很长的一句话，", result);
    }

    @Test
    @DisplayName("truncate: 没有任何分隔符 — 硬截断到 maxLen")
    void truncate_hardCut() throws Exception {
        assertEquals("ABCDEFGH", invokeTruncate("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 8));
    }

    @Test
    @DisplayName("truncate: 分隔符在 maxLen/2 之前 → 不在该处断（避免摘要过短）")
    void truncate_separatorTooEarly() throws Exception {
        String result = invokeTruncate("A。然后很多很多很多的文本内容在这里", 10);
        assertEquals(10, result.length());
        assertFalse(result.endsWith("。"));
    }

    @Test
    @DisplayName("truncate: 顿号断开 — 技术栈枚举场景")
    void truncate_dunHaoBreak() throws Exception {
        String result = invokeTruncate("技术栈：Java、Python、Go 还有很多其他内容", 16);
        assertEquals("技术栈：Java、Python、", result);
    }

    /** 反射调私有 truncate，避免改方法可见性 */
    private String invokeTruncate(String text, int maxLen) throws Exception {
        Method m = ExperienceSummaryService.class.getDeclaredMethod("truncate", String.class, int.class);
        m.setAccessible(true);
        return (String) m.invoke(service, text, maxLen);
    }
}
