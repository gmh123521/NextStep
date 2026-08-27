package com.nextstep.crawler.dto;

import lombok.Data;

/** 单次采集的统计结果 */
@Data
public class CrawlResult {
    private int fetched;
    private int inserted;
    private int skipped;

    public void addFetched()  { fetched++; }
    public void addInserted() { inserted++; }
    public void addSkipped()  { skipped++; }

    public String summary() {
        return String.format("抓取 %d 条，新入库 %d 条，重复跳过 %d 条", fetched, inserted, skipped);
    }
}
