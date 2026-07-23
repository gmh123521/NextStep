package com.nextstep.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 ChatService 中新增的纯逻辑方法。
 * 不使用 Mockito（Java 23 ByteBuddy 不兼容），直接传 null 给未用到的依赖。
 */
class ChatServiceTest {

    ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(null, null, null, null, null, null);
    }

    // ══════════════════════════════════════════════════════════
    // truncateText
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("truncateText: null 或空返回 null")
    void truncateText_nullOrBlank() {
        assertNull(chatService.truncateText(null, 200));
        assertNull(chatService.truncateText("", 200));
        assertNull(chatService.truncateText("   ", 200));
    }

    @Test
    @DisplayName("truncateText: 短文本原样返回")
    void truncateText_short() {
        assertEquals("Hello World", chatService.truncateText("Hello World", 200));
    }

    @Test
    @DisplayName("truncateText: 在句号处断开（需超过 maxLen/2 位置）")
    void truncateText_sentenceBreak() {
        String result = chatService.truncateText("这是第一句。这是第二句会被截断的部分", 9);
        assertEquals("这是第一句。", result);
    }

    @Test
    @DisplayName("truncateText: 句号太远，降到逗号")
    void truncateText_commaBreak() {
        String result = chatService.truncateText("很长很长的一句话，在这里有个逗号然后是更多内容", 14);
        assertEquals("很长很长的一句话，", result);
    }

    @Test
    @DisplayName("truncateText: 无任何分隔符时硬截断")
    void truncateText_hardCut() {
        String result = chatService.truncateText("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 10);
        assertEquals("ABCDEFGHIJ", result);
    }

    @Test
    @DisplayName("truncateText: 截取后长度不超过 maxLen")
    void truncateText_lengthContract() {
        String text = "a".repeat(500);
        String result = chatService.truncateText(text, 200);
        assertTrue(result.length() <= 200);
    }

    @Test
    @DisplayName("truncateText: 边界值 maxLen 等于文本长度")
    void truncateText_exactBoundary() {
        String text = "正好十个字啊哈";
        assertEquals(text, chatService.truncateText(text, text.length()));
    }

    // ══════════════════════════════════════════════════════════
    // matchExpKeywords
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("matchExpKeywords: null 或空返回 false")
    void matchExpKeywords_nullOrEmpty() {
        assertFalse(chatService.matchExpKeywords(null));
        assertFalse(chatService.matchExpKeywords(""));
    }

    @Test
    @DisplayName("matchExpKeywords: 经历相关词全部命中")
    void matchExpKeywords_hit() {
        assertTrue(chatService.matchExpKeywords("我之前的实习经历"));
        assertTrue(chatService.matchExpKeywords("有个项目想聊聊"));
        assertTrue(chatService.matchExpKeywords("简历上写了"));
        assertTrue(chatService.matchExpKeywords("我在字节干过"));
        assertTrue(chatService.matchExpKeywords("去年参加了一个比赛"));
        assertTrue(chatService.matchExpKeywords("发过一篇论文"));
        assertTrue(chatService.matchExpKeywords("做过科研"));
        assertTrue(chatService.matchExpKeywords("以前在腾讯"));
        assertTrue(chatService.matchExpKeywords("之前做过"));
        assertTrue(chatService.matchExpKeywords("竞赛"));
        assertTrue(chatService.matchExpKeywords("获奖"));
    }

    @Test
    @DisplayName("matchExpKeywords: 无经历关键词返回 false")
    void matchExpKeywords_miss() {
        assertFalse(chatService.matchExpKeywords("今天天气不错"));
        assertFalse(chatService.matchExpKeywords("考研应该怎么准备"));
        assertFalse(chatService.matchExpKeywords("我GPA不高怎么办"));
        assertFalse(chatService.matchExpKeywords("英语四级没过"));
    }

    @Test
    @DisplayName("matchExpKeywords: 公司名匹配")
    void matchExpKeywords_companyNames() {
        assertTrue(chatService.matchExpKeywords("字节面试"));
        assertTrue(chatService.matchExpKeywords("在腾讯"));
        assertTrue(chatService.matchExpKeywords("阿里云"));
        assertTrue(chatService.matchExpKeywords("百度实习"));
        assertTrue(chatService.matchExpKeywords("美团点评"));
        assertTrue(chatService.matchExpKeywords("京东物流"));
        assertTrue(chatService.matchExpKeywords("网易游戏"));
    }

    // ══════════════════════════════════════════════════════════
    // renderExperienceSnapshot (需要 mock mapper，null mapper 时 NPE 是预期的)
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("renderExperienceSnapshot: null mapper 应抛 NPE")
    void renderExperienceSnapshot_nullMapper() {
        assertThrows(NullPointerException.class,
                () -> chatService.renderExperienceSnapshot(1L, false));
    }
}