package com.nextstep.ai.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 与 OpenAI / DashScope 兼容的 chat completion 请求/响应模型
 * 故意手写而不引入 SDK：保持依赖最小、协议透明
 */
public class ChatModels {

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer max_tokens;
        private Boolean stream;
        /** 阿里 qwen3.x 系列：关闭思考模式让首字更快 */
        private Boolean enable_thinking;
        /** Function Calling 工具列表 */
        private List<Tool> tools;
        /** auto / required / none */
        private String tool_choice;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;     // system / user / assistant / tool
        private Object content;  // String 或 List<ContentPart>（多模态时）
        /** assistant 消息可能带 tool_calls */
        private List<ToolCall> tool_calls;
        /** role=tool 时的关联 id */
        private String tool_call_id;
        /** role=tool 时函数名 */
        private String name;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public Message(String role, Object content) {
            this.role = role;
            this.content = content;
        }
    }

    /** 多模态消息内容片段 */
    @Data
    public static class ContentPart {
        private String type;  // "text" / "image_url"
        private String text;
        private ImageUrl image_url;

        public static ContentPart text(String text) {
            ContentPart p = new ContentPart();
            p.type = "text"; p.text = text;
            return p;
        }
        public static ContentPart image(String url) {
            ContentPart p = new ContentPart();
            p.type = "image_url";
            p.image_url = new ImageUrl();
            p.image_url.url = url;
            return p;
        }
    }

    @Data
    public static class ImageUrl {
        private String url;
    }

    @Data
    public static class ChatResponse {
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private Delta delta;
        private String finish_reason;
    }

    /** OpenAI 兼容的 Tool 定义 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        private String type = "function";
        private Function function;

        public Tool() {}
        public Tool(Function function) {
            this.type = "function";
            this.function = function;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String description;
        /** JSON Schema 描述 parameters */
        private java.util.Map<String, Object> parameters;
    }

    /** LLM 决定调用某个工具时返回 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCall {
        private String id;
        private String type = "function";
        private FunctionCall function;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionCall {
        private String name;
        /** JSON 字符串（OpenAI 协议） */
        private String arguments;
    }

    /** 流式响应中的增量片段 */
    @Data
    public static class Delta {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}
