package com.nextstep.crawler.source;

import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.mapper.SchoolUpsertMapper;
import com.nextstep.crawler.service.DataImportBatchService;
import com.nextstep.crawler.service.RawSnapshotStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KaoyanCrawlerTest {

    private final CrawlerProperties props = new CrawlerProperties();
    private final HttpFetcher fetcher = mock(HttpFetcher.class);
    private final SchoolUpsertMapper mapper = mock(SchoolUpsertMapper.class);
    private KaoyanCrawler crawler;

    @BeforeEach
    void setUp() {
        props.setKaoyanUrl("https://example.test/schools");
        props.setMaxItems(10);
        crawler = new KaoyanCrawler(props, fetcher, mapper);
    }

    @Test
    void rejectsInvalidJsonInsteadOfReportingEmptySuccess() {
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("<html>blocked</html>");

        BizException error = assertThrows(BizException.class, crawler::crawl);

        assertEquals("研招院校数据响应格式无效", error.getMessage());
    }

    @Test
    void rejectsJsonWithoutSchoolArray() {
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("{\"code\":200,\"message\":\"ok\"}");

        BizException error = assertThrows(BizException.class, crawler::crawl);

        assertEquals("研招院校数据缺少列表节点", error.getMessage());
    }

    @Test
    void countsInsertedDuplicateAndInvalidRows() {
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("""
                {"data":[
                  {"dwmc":"甲大学","dwdm":"10001","ssmc":"北京","is985":"1"},
                  {"dwmc":"乙大学","dwdm":"10002","ssmc":"上海","is211":"1"},
                  {"dwdm":"10003","ssmc":"广东"}
                ]}
                """);
        when(mapper.insertIgnore(any())).thenReturn(1, 0);

        CrawlResult result = crawler.crawl();

        assertEquals(3, result.getFetched());
        assertEquals(1, result.getInserted());
        assertEquals(2, result.getSkipped());
    }

    @Test
    void supportsSchoolArrayNestedInsideDataObject() {
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("""
                {"data":{"schools":[{"schoolName":"嵌套大学","schoolCode":"20001"}]}}
                """);
        when(mapper.insertIgnore(any())).thenReturn(1);

        CrawlResult result = crawler.crawl();

        assertEquals(1, result.getFetched());
        assertEquals(1, result.getInserted());
    }

    @Test
    void savesRawResponseSnapshotBeforeParsing() {
        RawSnapshotStore snapshotStore = mock(RawSnapshotStore.class);
        crawler = new KaoyanCrawler(props, fetcher, mapper, snapshotStore);
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("{\"data\":[{\"dwmc\":\"快照大学\",\"dwdm\":\"30001\"}]}");
        when(mapper.insertIgnore(any())).thenReturn(1);

        crawler.crawl();

        verify(snapshotStore).save(eq("KAOYAN_SCHOOL"), eq(props.getKaoyanDataYear()), any(), any());
    }

    @Test
    void createsAndCompletesImportBatch() {
        RawSnapshotStore snapshotStore = mock(RawSnapshotStore.class);
        DataImportBatchService batchService = mock(DataImportBatchService.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(21L);
        batch.setStatus("PENDING");
        when(batchService.createOrReuse(eq("KAOYAN_SCHOOL"), eq(props.getKaoyanDataYear()), any(), eq("v1"))).thenReturn(batch);
        when(snapshotStore.save(any(), eq(props.getKaoyanDataYear()), any(), any())).thenReturn("KAOYAN_SCHOOL/2026/hash.json");
        when(fetcher.get(props.getKaoyanUrl())).thenReturn("{\"data\":[{\"dwmc\":\"批次大学\",\"dwdm\":\"40001\"}]}");
        when(mapper.insertIgnore(any())).thenReturn(1);
        crawler = new KaoyanCrawler(props, fetcher, mapper, snapshotStore, batchService);

        crawler.crawl();

        verify(batchService).markRunning(21L);
        verify(batchService).markSucceeded(21L, 1, 1, 0, 0);
    }
}
