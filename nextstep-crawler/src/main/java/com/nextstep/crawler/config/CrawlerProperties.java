package com.nextstep.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采集配置。数据源 URL 可通过环境变量覆盖，默认指向真实公开站点。
 * 生产环境请遵守各站点 robots 协议与爬取频率限制。
 */
@Data
@Component
@ConfigurationProperties(prefix = "nextstep.crawler")
public class CrawlerProperties {

    /** 总开关：定时任务是否启用 */
    private boolean enabled = false;

    /** 单次请求间隔（毫秒），礼貌爬取 */
    private long rateLimitMs = 1500;

    /** 请求超时（秒） */
    private int timeoutSeconds = 15;

    /** 单次任务最大抓取条数（防止意外大批量） */
    private int maxItems = 500;

    /** 研招网：招生专业目录 JSON 接口（示例，实际以站点公开接口为准） */
    private String kaoyanUrl = "https://yz.chsi.com.cn/zsml/pages/getMl.jsp";

    /** 国家公务员局：职位表数据接口 */
    private String govPostUrl = "http://www.scs.gov.cn/kl2023/kl/zwb/index.json";

    /** UA 伪装池，逐次轮换 */
    private List<String> userAgents = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36"
    );
}
