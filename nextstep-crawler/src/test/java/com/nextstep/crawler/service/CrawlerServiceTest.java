package com.nextstep.crawler.service;

import com.nextstep.crawler.entity.CrawlerJob;
import com.nextstep.crawler.mapper.CrawlerJobMapper;
import com.nextstep.crawler.source.SourceCrawler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlerServiceTest {

    @Test
    void recordsFailedJobWhenSourceParserRejectsResponse() {
        CrawlerJobMapper mapper = mock(CrawlerJobMapper.class);
        SourceCrawler crawler = mock(SourceCrawler.class);
        when(crawler.source()).thenReturn("KAOYAN");
        when(crawler.crawl()).thenThrow(new IllegalStateException("响应格式变化"));
        doAnswer(invocation -> {
            CrawlerJob job = invocation.getArgument(0);
            job.setId(1L);
            return 1;
        }).when(mapper).insert(any(CrawlerJob.class));
        CrawlerService service = new CrawlerService(mapper, List.of(crawler));

        CrawlerJob job = service.run("kaoyan", "MANUAL");

        assertEquals("FAILED", job.getStatus());
        assertEquals("KAOYAN", job.getSource());
        assertNotNull(job.getFinishedAt());
        verify(mapper).updateById(job);
    }
}
