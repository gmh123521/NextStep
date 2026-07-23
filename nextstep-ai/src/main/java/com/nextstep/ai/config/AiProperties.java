package com.nextstep.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼（DashScope）配置
 * 使用 OpenAI 兼容模式：https://dashscope.aliyuncs.com/compatible-mode/v1
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "nextstep.ai")
public class AiProperties {

    /** API key（从环境变量 DASHSCOPE_API_KEY 注入） */
    private String apiKey;

    /** 兼容模式 base url */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 文本对话模型（百炼真实可用：qwen-turbo / qwen-plus / qwen-max / qwen3-32b 等） */
    private String textModel = "qwen3.7-plus";

    /** 多模态视觉模型（百炼真实可用：qwen-vl-plus / qwen-vl-max / qwen-vl-ocr） */
    private String visionModel = "qwen3.6-plus";

    /** 单次请求最长输出 token */
    private int maxTokens = 2048;

    /** 默认 temperature */
    private double temperature = 0.7;

    /** 请求总超时（秒） */
    private int timeoutSeconds = 60;
}
