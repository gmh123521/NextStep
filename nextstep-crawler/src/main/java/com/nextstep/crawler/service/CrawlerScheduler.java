package com.nextstep.crawler.service;

import com.nextstep.crawler.config.CrawlerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时采集触发器。默认关闭（nextstep.crawler.enabled=false），
 * 仅当显式开启时才注册定时任务，避免开发/CI 环境误触发外网请求。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nextstep.crawler", name = "enabled", havingValue = "true")
public class CrawlerScheduler {

    private final CrawlerService crawlerService;
    private final CrawlerProperties props;

    /** 每天凌晨 3 点执行全量数据源采集 */
    @Scheduled(cron = "${nextstep.crawler.cron:0 0 3 * * ?}")
    public void scheduledCrawl() {
        if (!props.isEnabled()) return;
        log.info("[crawler] 定时采集启动");
        crawlerService.runAll("SCHEDULE");
        log.info("[crawler] 定时采集结束");
    }
}
