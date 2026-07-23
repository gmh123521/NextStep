package com.nextstep.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 ResumeExtractService 中保留的纯逻辑方法。
 * 摘要逻辑已迁移到 ExperienceSummaryService（独立测试），这里只保留指纹/去重相关纯逻辑。
 */
class ResumeExtractServiceTest {

    ResumeExtractService service;

    @BeforeEach
    void setUp() {
        service = new ResumeExtractService(null, null, null, null);
    }

    @Test
    @DisplayName("占位：构造未抛异常")
    void constructible() {
        assertNotNull(service);
    }
}
