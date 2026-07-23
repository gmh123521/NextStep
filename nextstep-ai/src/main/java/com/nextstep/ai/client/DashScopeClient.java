package com.nextstep.ai.client;

import com.nextstep.ai.client.ChatModels.*;
import com.nextstep.ai.config.AiProperties;
import com.nextstep.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

/**
 * 阿里云百炼 LLM 客户端（OpenAI 兼容模式）
 * 同时支持：同步 chat / 流式 stream / 多模态 vision
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeClient {

    private final AiProperties props;
    private WebClient webClient;

    private WebClient client() {
        if (webClient == null) {
            HttpClient http = HttpClient.create().responseTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
            webClient = WebClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                    .clientConnector(new ReactorClientHttpConnector(http))
                    // 多模态 base64 图片可能很大，给 32MB 缓冲
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                    .build();
        }
        return webClient;
    }

    /** 同步 chat */
    public String chat(List<Message> messages) {
        return chat(messages, props.getTextModel());
    }

    public String chat(List<Message> messages, String model) {
        validate();
        ChatModels.Choice choice = chatRaw(messages, model, null, null);
        return choice.getMessage() == null
                ? "" : asText(choice.getMessage().getContent());
    }

    /** 长输出场景（如规划生成）单独传更高的 max_tokens，避免被默认 4096 截断 */
    public String chatWithMaxTokens(List<Message> messages, int maxTokens) {
        validate();
        ChatModels.Choice choice = chatRaw(messages, props.getTextModel(), null, maxTokens);
        return choice.getMessage() == null
                ? "" : asText(choice.getMessage().getContent());
    }

    /** 带 Tools 的同步调用，返回完整 Choice（可能含 tool_calls） */
    public ChatModels.Choice chatWithTools(List<Message> messages, List<ChatModels.Tool> tools) {
        return chatRaw(messages, props.getTextModel(), tools, null);
    }

    private ChatModels.Choice chatRaw(List<Message> messages, String model,
                                      List<ChatModels.Tool> tools, Integer maxTokensOverride) {
        validate();
        ChatRequest req = new ChatRequest();
        req.setModel(model);
        req.setMessages(messages);
        req.setTemperature(props.getTemperature());
        boolean isVision = model != null && model.toLowerCase().contains("vl") || model != null && model.equals(props.getVisionModel());
        int defaultTokens = isVision ? 4096 : props.getMaxTokens();
        req.setMax_tokens(maxTokensOverride != null ? maxTokensOverride : defaultTokens);
        req.setStream(false);
        req.setEnable_thinking(false);
        if (tools != null && !tools.isEmpty()) {
            req.setTools(tools);
            req.setTool_choice("auto");
        }

        ChatResponse resp = client().post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.isError(),
                        r -> r.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("LLM HTTP {} body: {}", r.statusCode(), body);
                                    return reactor.core.publisher.Mono.error(
                                            new BizException("LLM 调用失败: " + r.statusCode() + " - " + body));
                                }))
                .bodyToMono(ChatResponse.class)
                .block(java.time.Duration.ofSeconds(props.getTimeoutSeconds()));

        if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) {
            throw new BizException("LLM 无返回");
        }
        return resp.getChoices().get(0);
    }

    private String asText(Object content) {
        return content == null ? "" : content.toString();
    }

    /** 流式 chat：返回 Flux<String> 给上层做 SSE 推送 */
    public Flux<String> stream(List<Message> messages) {
        return stream(messages, props.getTextModel());
    }

    public Flux<String> stream(List<Message> messages, String model) {
        validate();
        ChatRequest req = new ChatRequest();
        req.setModel(model);
        req.setMessages(messages);
        req.setTemperature(props.getTemperature());
        req.setMax_tokens(props.getMaxTokens());
        req.setStream(true);
        // 关闭思考模式 → 首字延迟从 3-5s 降到 0.5-1s
        req.setEnable_thinking(false);

        return client().post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("LLM stream HTTP {} body: {}", resp.statusCode(), body);
                                    return reactor.core.publisher.Mono.error(
                                            new BizException("LLM 调用失败: " + resp.statusCode() + " - " + body));
                                }))
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank() && !"[DONE]".equals(line.trim()))
                // 用 handle 替代 map+filter：遇到 null 静默跳过，不会因 NPE 中断流
                .<String>handle((line, sink) -> {
                    String content = parseDeltaContent(line);
                    if (content != null && !content.isEmpty()) sink.next(content);
                })
                .doOnError(e -> log.error("LLM stream error: {}", e.getMessage()));
    }

    /** 多模态 chat（图片识别）— 接收图片 URL */
    public String vision(String prompt, List<String> imageUrls) {
        validate();
        var parts = new java.util.ArrayList<ContentPart>();
        parts.add(ContentPart.text(prompt));
        for (String url : imageUrls) parts.add(ContentPart.image(url));

        Message msg = new Message();
        msg.setRole("user");
        msg.setContent(parts);

        return chat(List.of(msg), props.getVisionModel());
    }

    /** 多模态 chat（图片识别）— 直接接收 base64 字节，自动转 data URL */
    public String visionBytes(String prompt, byte[] imageBytes, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64,"
                + java.util.Base64.getEncoder().encodeToString(imageBytes);
        return vision(prompt, List.of(dataUrl));
    }

    /** 解析 SSE 单行：data 行 JSON 中提取 delta.content */
    private String parseDeltaContent(String line) {
        try {
            // 用简单字符串提取避免引入 jackson 配置
            int idx = line.indexOf("\"content\":\"");
            if (idx < 0) return null;
            int start = idx + "\"content\":\"".length();
            int end = start;
            StringBuilder sb = new StringBuilder();
            while (end < line.length()) {
                char c = line.charAt(end);
                if (c == '\\' && end + 1 < line.length()) {
                    char next = line.charAt(end + 1);
                    sb.append(switch (next) {
                        case 'n'  -> '\n';
                        case 't'  -> '\t';
                        case 'r'  -> '\r';
                        case '"'  -> '"';
                        case '\\' -> '\\';
                        default   -> next;
                    });
                    end += 2;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    end++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void validate() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BizException("未配置 nextstep.ai.api-key（或环境变量 DASHSCOPE_API_KEY）");
        }
    }
}
