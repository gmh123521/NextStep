package com.nextstep.crawler.source;

import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.mapper.GovPostUpsertMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GovPostCrawlerTest {

    private final CrawlerProperties props = new CrawlerProperties();
    private final HttpFetcher fetcher = mock(HttpFetcher.class);
    private final GovPostUpsertMapper mapper = mock(GovPostUpsertMapper.class);
    private GovPostCrawler crawler;

    @BeforeEach
    void setUp() {
        props.setGovPostUrl("https://example.test/posts");
        props.setMaxItems(10);
        crawler = new GovPostCrawler(props, fetcher, mapper);
    }

    @Test
    void rejectsEmptyResponseInsteadOfReportingEmptySuccess() {
        when(fetcher.get(props.getGovPostUrl())).thenReturn(" ");

        BizException error = assertThrows(BizException.class, crawler::crawl);

        assertEquals("国考岗位数据响应为空", error.getMessage());
    }

    @Test
    void parsesRowsAndSkipsRecordsMissingRequiredFields() {
        when(fetcher.get(props.getGovPostUrl())).thenReturn("""
                {"rows":[
                  {"ksnd":"2026年","zsjg":"中央机关","zwbm":"001","zwmc":"综合管理"},
                  {"ksnd":"2026年","zwmc":"缺少部门"}
                ]}
                """);
        when(mapper.insertIgnore(any())).thenReturn(1);

        CrawlResult result = crawler.crawl();

        assertEquals(2, result.getFetched());
        assertEquals(1, result.getInserted());
        assertEquals(1, result.getSkipped());
    }

    @Test
    void supportsPositionArrayNestedInsideDataObject() {
        when(fetcher.get(props.getGovPostUrl())).thenReturn("""
                {"data":{"rows":[{"year":2026,"deptName":"直属机构","postCode":"002","postName":"业务管理"}]}}
                """);
        when(mapper.insertIgnore(any())).thenReturn(1);

        CrawlResult result = crawler.crawl();

        assertEquals(1, result.getFetched());
        assertEquals(1, result.getInserted());
    }
}
