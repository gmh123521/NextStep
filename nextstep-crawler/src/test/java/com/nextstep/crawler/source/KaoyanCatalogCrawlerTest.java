package com.nextstep.crawler.source;

import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.fetch.HttpFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KaoyanCatalogCrawlerTest {

    @Test
    void postsChsiFormAndCountsCatalogRows() {
        CrawlerProperties props = new CrawlerProperties();
        props.setKaoyanCatalogUrl("https://example.test/zys.do");
        props.setMaxItems(10);
        HttpFetcher fetcher = mock(HttpFetcher.class);
        when(fetcher.postForm(eq(props.getKaoyanCatalogUrl()), any(MultiValueMap.class), anyMap())).thenReturn("""
                {
                  "msg":{"curPage":1,"nextPageAvailable":false,"totalCount":1,
                    "list":[{"zydm":"081200","zymc":"计算机科学与技术","mldm":"08",
                      "mlmc":"工学","yjxkdm":"0812","xwlxmc":"学术学位"}]},
                  "flag":true,"invokeStatus":"SUCCESS"
                }
                """);

        KaoyanCatalogCrawler crawler = new KaoyanCatalogCrawler(
                props, fetcher, new KaoyanCatalogParser(), null, null, null);

        CrawlResult result = crawler.crawl();

        assertEquals("KAOYAN_CATALOG", crawler.source());
        assertEquals(1, result.getFetched());
        assertEquals(1, result.getInserted());
        verify(fetcher).postForm(eq(props.getKaoyanCatalogUrl()), argThat(form ->
                "0".equals(form.getFirst("start"))
                        && "1".equals(form.getFirst("curPage"))
                        && "10".equals(form.getFirst("pageSize"))
                        && "".equals(form.getFirst("zydm"))), anyMap());
    }
}
