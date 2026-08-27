package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.entity.CrawlerJob;
import com.nextstep.crawler.mapper.CrawlerJobMapper;
import com.nextstep.crawler.source.SourceCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采集编排：按数据源分发到对应 {@link SourceCrawler}，
 * 每次运行落一条 {@link CrawlerJob} 记录（RUNNING → SUCCESS/FAILED）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final CrawlerJobMapper crawlerJobMapper;
    private final List<SourceCrawler> crawlers;

    private Map<String, SourceCrawler> index() {
        return crawlers.stream().collect(Collectors.toMap(SourceCrawler::source, Function.identity()));
    }

    /** 可用数据源标识 */
    public List<String> sources() {
        return crawlers.stream().map(SourceCrawler::source).toList();
    }

    /** 运行指定数据源采集，triggerBy: SCHEDULE / MANUAL */
    public CrawlerJob run(String source, String triggerBy) {
        String normalizedSource = source == null ? "" : source.trim().toUpperCase();
        SourceCrawler crawler = index().get(normalizedSource);
        if (crawler == null) throw new BizException("未知数据源：" + source);

        CrawlerJob job = new CrawlerJob();
        job.setSource(normalizedSource);
        job.setTriggerBy("SCHEDULE".equalsIgnoreCase(triggerBy) ? "SCHEDULE" : "MANUAL");
        job.setStatus("RUNNING");
        job.setFetched(0);
        job.setInserted(0);
        job.setSkipped(0);
        job.setStartedAt(LocalDateTime.now());
        crawlerJobMapper.insert(job);

        try {
            CrawlResult r = crawler.crawl();
            job.setFetched(r.getFetched());
            job.setInserted(r.getInserted());
            job.setSkipped(r.getSkipped());
            job.setStatus("SUCCESS");
            job.setMessage(r.summary());
        } catch (Exception e) {
            log.error("[crawler] {} 采集失败: {}", source, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setMessage(truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            crawlerJobMapper.updateById(job);
        }
        return job;
    }

    /** 运行全部数据源（定时任务用） */
    public void runAll(String triggerBy) {
        for (String source : sources()) {
            try {
                run(source, triggerBy);
            } catch (Exception e) {
                log.error("[crawler] 数据源 {} 执行异常: {}", source, e.getMessage());
            }
        }
    }

    /** 任务运行记录分页 */
    public PageResult<CrawlerJob> pageJobs(int pageNum, int pageSize, String source) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数非法：页码必须大于 0，页大小为 1-200");
        }
        String normalizedSource = source == null ? null : source.trim().toUpperCase();
        if (normalizedSource != null && !normalizedSource.isBlank() && !index().containsKey(normalizedSource)) {
            throw new BizException("未知数据源：" + source);
        }
        LambdaQueryWrapper<CrawlerJob> w = new LambdaQueryWrapper<CrawlerJob>()
                .eq(normalizedSource != null && !normalizedSource.isBlank(), CrawlerJob::getSource, normalizedSource)
                .orderByDesc(CrawlerJob::getId);
        Page<CrawlerJob> p = crawlerJobMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    private String truncate(String s) {
        if (s == null) return "未知错误";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
