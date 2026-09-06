package com.nextstep.crawler.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.service.DataImportBatchService;
import com.nextstep.crawler.service.DataSourceService;
import com.nextstep.crawler.service.RawSnapshotStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 研招网专业目录真实 POST 接口适配器。 */
@Slf4j
@Component
public class KaoyanCatalogCrawler implements SourceCrawler {

    private static final String SOURCE = "KAOYAN_CATALOG";
    private static final String PARSER_VERSION = "v2";

    private final CrawlerProperties props;
    private final HttpFetcher fetcher;
    private final KaoyanCatalogParser parser;
    private final RawSnapshotStore snapshotStore;
    private final DataImportBatchService batchService;
    private final DataSourceService dataSourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KaoyanCatalogCrawler(CrawlerProperties props, HttpFetcher fetcher, KaoyanCatalogParser parser) {
        this(props, fetcher, parser, null, null, null);
    }

    @Autowired
    public KaoyanCatalogCrawler(CrawlerProperties props, HttpFetcher fetcher, KaoyanCatalogParser parser,
                                RawSnapshotStore snapshotStore, DataImportBatchService batchService,
                                DataSourceService dataSourceService) {
        this.props = props;
        this.fetcher = fetcher;
        this.parser = parser;
        this.snapshotStore = snapshotStore;
        this.batchService = batchService;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public CrawlResult crawl() {
        String url = dataSourceService == null
                ? props.getKaoyanCatalogUrl()
                : dataSourceService.resolveUrl(SOURCE, props.getKaoyanCatalogUrl());
        int pageSize = Math.max(1, Math.min(100, props.getMaxItems()));
        int page = 1;
        int start = 0;
        int fetchedPages = 0;
        List<String> responses = new ArrayList<>();
        CrawlResult result = new CrawlResult();

        while (result.getFetched() < props.getMaxItems()) {
            MultiValueMap<String, String> form = form(start, page, pageSize);
            String body = fetcher.postForm(url, form, Map.of("Referer", "https://yz.chsi.com.cn/zsml/"));
            if (body == null || body.isBlank()) throw new BizException("研招网专业目录响应为空");
            if (!parser.successful(body)) throw new BizException("研招网专业目录接口返回失败");
            responses.add(body);
            fetchedPages++;

            KaoyanCatalogParser.ParseResult parsed = parser.parse(body, props.getKaoyanDataYear());
            int rows = parsed.rowCount();
            result.setFetched(result.getFetched() + rows);
            result.setInserted(result.getInserted() + parsed.records().size());
            result.setSkipped(result.getSkipped() + Math.max(0, rows - parsed.records().size()));

            if (!parser.nextPageAvailable(body) || rows == 0) break;
            start += pageSize;
            page++;
        }

        completeBatch(url, responses, result);
        log.info("[crawler:{}] 抓取 {} 页，{}", SOURCE, fetchedPages, result.summary());
        return result;
    }

    private MultiValueMap<String, String> form(int start, int page, int pageSize) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("zydm", "");
        form.add("zymc", "");
        form.add("xwlx", "");
        form.add("mldm", "");
        form.add("yjxkdm", "");
        form.add("xxfs", "");
        form.add("tydxs", "");
        form.add("jsggjh", "");
        form.add("start", String.valueOf(start));
        form.add("curPage", String.valueOf(page));
        form.add("pageSize", String.valueOf(pageSize));
        form.add("totalPage", "0");
        form.add("totalCount", "0");
        return form;
    }

    private void completeBatch(String url, List<String> responses, CrawlResult result) {
        if (batchService == null || responses.isEmpty()) return;
        byte[] snapshot = objectMapper.createArrayNode().addAll(
                responses.stream().map(this::readTree).toList()).toString().getBytes(StandardCharsets.UTF_8);
        String hash = sha256(snapshot);
        String path = snapshotStore == null ? null : snapshotStore.save(SOURCE, props.getKaoyanDataYear(), hash, snapshot);
        DataImportBatch batch = batchService.createOrReuse(SOURCE, props.getKaoyanDataYear(), "sha256:" + hash, PARSER_VERSION);
        if (path != null) batchService.attachSnapshot(batch.getId(), url, path);
        boolean alreadyProcessed = java.util.Set.of("SUCCEEDED", "APPROVED", "PUBLISHED").contains(batch.getStatus());
        if (alreadyProcessed) return;
        batchService.clearRawRecords(batch.getId());
        batchService.markRunning(batch.getId());
        boolean fatalParseError = false;
        String firstParseError = null;
        for (int i = 0; i < responses.size(); i++) {
            String pageBody = responses.get(i);
            KaoyanCatalogParser.ParseResult parsed = parser.parse(pageBody, props.getKaoyanDataYear());
            String error = parsed.errors().isEmpty() ? null : String.join("; ", parsed.errors());
            if (parsed.records().isEmpty() && !parsed.errors().isEmpty()) {
                fatalParseError = true;
                if (firstParseError == null) firstParseError = error;
            }
            batchService.saveRawRecord(batch.getId(), i + 1, pageBody, normalizedPayload(parsed),
                    sha256(pageBody.getBytes(StandardCharsets.UTF_8)),
                    parsed.records().isEmpty() ? "FAILED" : "SUCCESS", error);
        }
        if (fatalParseError) {
            String message = firstParseError == null ? "专业目录解析失败" : firstParseError;
            batchService.markFailed(batch.getId(), message);
            throw new BizException(message);
        } else {
            batchService.markSucceeded(batch.getId(), result.getFetched(), result.getInserted(), result.getSkipped(), 0);
        }
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("raw", body);
        }
    }

    private String normalizedPayload(KaoyanCatalogParser.ParseResult parsed) {
        try {
            return objectMapper.writeValueAsString(parsed.records());
        } catch (Exception e) {
            throw new IllegalStateException("标准化专业目录序列化失败", e);
        }
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
