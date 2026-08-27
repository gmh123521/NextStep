package com.nextstep.crawler.source;

import com.nextstep.crawler.dto.CrawlResult;

/** 数据源采集器：一次完整的抓取-解析-入库流程 */
public interface SourceCrawler {

    /** 数据源标识，与 ns_crawler_job.source 对应 */
    String source();

    /** 执行采集，返回统计结果；异常向上抛出由调度层记录 FAILED */
    CrawlResult crawl();
}
