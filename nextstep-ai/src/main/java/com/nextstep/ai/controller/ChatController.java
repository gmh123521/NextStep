package com.nextstep.ai.controller;

import com.nextstep.ai.service.ChatService;
import com.nextstep.ai.service.ChatService.ChatTurnResult;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI 对话")
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送一句话，AI 会自动调工具改画像并回复")
    @PostMapping
    public R<ChatTurnResult> chat(@RequestBody ChatRequest req) {
        return R.ok(chatService.chat(SecurityUtils.currentUserId(), req.getMessage()));
    }

    @Operation(summary = "开场白：让 AI 看着画像主动开口（首次进入对话时调用）")
    @PostMapping("/kickoff")
    public R<ChatTurnResult> kickoff() {
        return R.ok(chatService.kickoff(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "查询历史对话")
    @GetMapping("/history")
    public R<List<?>> history() {
        return R.ok(chatService.getHistoryForView(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "重置对话（清空历史）")
    @DeleteMapping("/history")
    public R<Void> reset() {
        chatService.resetHistory(SecurityUtils.currentUserId());
        return R.ok();
    }

    @Data
    public static class ChatRequest {
        @NotBlank
        private String message;
    }
}
