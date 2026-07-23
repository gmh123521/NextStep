package com.nextstep.ai.controller;

import com.nextstep.ai.service.ExplainService;
import com.nextstep.common.core.R;
import com.nextstep.framework.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "AI 解读")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final ExplainService explainService;

    @Operation(summary = "评分解读（SSE 流式）")
    @GetMapping(value = "/explain", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> explain() {
        Long userId = SecurityUtils.currentUserId();
        return explainService.explainStream(userId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done").data("[DONE]").build()));
    }

    @Operation(summary = "读取最近一次评分解读缓存（无则 data=null）")
    @GetMapping("/explain/cache")
    public R<String> explainCache() {
        return R.ok(explainService.getCached(SecurityUtils.currentUserId()));
    }
}
