package com.nextstep.crawler.fetch;

import com.nextstep.crawler.config.CrawlerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用 HTTP 抓取器：WebClient 拉取 + UA 轮换 + 礼貌限速 + 一次重试。
 * 返回原始响应体字符串，交由各数据源解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpFetcher {

    private final CrawlerProperties props;
    private final AtomicInteger uaCursor = new AtomicInteger(0);
    private volatile WebClient webClient;

    private WebClient client() {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    HttpClient http = HttpClient.create()
                            .responseTimeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                            .followRedirect(true);
                    webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(http))
                            .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                            .build();
                }
            }
        }
        return webClient;
    }

    /** 阻塞式抓取；限速后取一个 UA 发起 GET，失败重试一次。 */
    public String get(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("采集地址不能为空");
        if (props.getRateLimitMs() < 0) throw new IllegalStateException("采集限速不能为负数");
        rateLimit();
        try {
            return doGet(url);
        } catch (Exception e) {
            log.warn("[crawler] 抓取失败，重试一次: {} -> {}", url, e.getMessage());
            rateLimit();
            return doGet(url);
        }
    }

    private String doGet(String url) {
        return client().get()
                .uri(url)
                .header("User-Agent", nextUa())
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(props.getTimeoutSeconds() + 5L));
    }

    private String nextUa() {
        var uas = props.getUserAgents();
        if (uas == null || uas.isEmpty()) return "NextStepCrawler/1.0";
        int idx = Math.floorMod(uaCursor.getAndIncrement(), uas.size());
        return uas.get(idx);
    }

    private void rateLimit() {
        try {
            Thread.sleep(props.getRateLimitMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
