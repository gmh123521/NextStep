package com.nextstep.crawler.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.mapper.SchoolUpsertMapper;
import com.nextstep.crawler.service.DataImportBatchService;
import com.nextstep.crawler.service.DataSourceService;
import com.nextstep.crawler.service.RawSnapshotStore;
import com.nextstep.data.school.entity.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 * 研招网（yz.chsi.com.cn）招生单位采集。
 *
 * 站点以 JSON 接口返回招生单位列表（省份 / 院校名 / 院校代码 / 层次），本采集器：
 *   1. WebClient 拉取 JSON（UA 伪装 + 限速）
 *   2. Jackson 解析出招生单位
 *   3. INSERT IGNORE 写入 ns_school，按院校名去重
 *
 * 注意：研招网真实接口可能要求 referer / 分页参数 / 会话，字段名以线上响应为准；
 * 此处按公开列表接口的常见结构解析，字段缺失时安全跳过。
 */
@Slf4j
@Component
public class KaoyanCrawler implements SourceCrawler {

    private final CrawlerProperties props;
    private final HttpFetcher fetcher;
    private final SchoolUpsertMapper schoolUpsertMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RawSnapshotStore snapshotStore;
    private final DataImportBatchService batchService;
    private final DataSourceService dataSourceService;

    public KaoyanCrawler(CrawlerProperties props, HttpFetcher fetcher, SchoolUpsertMapper schoolUpsertMapper) {
        this(props, fetcher, schoolUpsertMapper, null, null, null);
    }

    public KaoyanCrawler(CrawlerProperties props, HttpFetcher fetcher, SchoolUpsertMapper schoolUpsertMapper,
                         RawSnapshotStore snapshotStore) {
        this(props, fetcher, schoolUpsertMapper, snapshotStore, null, null);
    }

    public KaoyanCrawler(CrawlerProperties props, HttpFetcher fetcher, SchoolUpsertMapper schoolUpsertMapper,
                         RawSnapshotStore snapshotStore, DataImportBatchService batchService) {
        this(props, fetcher, schoolUpsertMapper, snapshotStore, batchService, null);
    }

    @Autowired
    public KaoyanCrawler(CrawlerProperties props, HttpFetcher fetcher, SchoolUpsertMapper schoolUpsertMapper,
                         RawSnapshotStore snapshotStore, DataImportBatchService batchService,
                         DataSourceService dataSourceService) {
        this.props = props;
        this.fetcher = fetcher;
        this.schoolUpsertMapper = schoolUpsertMapper;
        this.snapshotStore = snapshotStore;
        this.batchService = batchService;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public String source() {
        return "KAOYAN";
    }

    @Override
    public CrawlResult crawl() {
        CrawlResult result = new CrawlResult();
        String sourceUrl = dataSourceService == null ? props.getKaoyanUrl()
                : dataSourceService.resolveUrl("KAOYAN_SCHOOL", props.getKaoyanUrl());
        String body = fetcher.get(sourceUrl);
        if (body == null || body.isBlank()) {
            throw new BizException("研招院校数据响应为空");
        }
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        String hash = sha256(content);
        String snapshotPath = saveSnapshot(body, hash);
        DataImportBatch batch = beginBatch(hash, snapshotPath, sourceUrl);

        try {
            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (Exception e) {
                throw new BizException("研招院校数据响应格式无效");
            }

        // 兼容常见结构：顶层数组，或 {data:[...]} / {ssdm:[...]}
        JsonNode list = firstArray(root, "data", "list", "yxList", "ssList");
            if (list == null || !list.isArray()) {
                throw new BizException("研招院校数据缺少列表节点");
            }

            for (JsonNode node : list) {
            if (result.getFetched() >= props.getMaxItems()) {
                log.info("[crawler:KAOYAN] 达到单次上限 {} 条，停止", props.getMaxItems());
                break;
            }
            result.addFetched();

            School s = toSchool(node);
            if (s == null || s.getName() == null || s.getName().isBlank()) {
                result.addSkipped();
                continue;
            }
            int affected = schoolUpsertMapper.insertIgnore(s);
            if (affected > 0) result.addInserted();
            else result.addSkipped();
            }
            if (batch != null) batchService.markSucceeded(batch.getId(), result.getFetched(), result.getInserted(), result.getSkipped(), 0);
            log.info("[crawler:KAOYAN] {}", result.summary());
            return result;
        } catch (RuntimeException e) {
            if (batch != null) batchService.markFailed(batch.getId(), e.getMessage());
            throw e;
        }
    }

    private School toSchool(JsonNode node) {
        School s = new School();
        s.setName(text(node, "dwmc", "yxmc", "name", "schoolName"));
        s.setCode(text(node, "dwdm", "yxdm", "code", "schoolCode"));
        s.setProvince(text(node, "ssmc", "province", "sf"));
        s.setLevel(deriveLevel(node));
        s.setType(text(node, "yxlx", "type"));
        s.setIsSelfMarking(0);
        return s;
    }

    /** 从「是否 985/211/双一流」标志推导层次 */
    private String deriveLevel(JsonNode node) {
        if ("1".equals(text(node, "is985")) || node.path("is985").asBoolean(false)) return "985";
        if ("1".equals(text(node, "is211")) || node.path("is211").asBoolean(false)) return "211";
        if ("1".equals(text(node, "isSyl")) || node.path("isSyl").asBoolean(false)) return "双一流";
        String lv = text(node, "level", "cc");
        return lv == null ? "普通本科" : lv;
    }

    private JsonNode firstArray(JsonNode root, String... keys) {
        if (root.isArray()) return root;
        for (String k : keys) {
            JsonNode n = root.get(k);
            if (n != null && n.isArray()) return n;
        }
        // 兼容 data.rows / data.schools 等嵌套结构
        Iterator<JsonNode> it = root.elements();
        while (it.hasNext()) {
            JsonNode child = it.next();
            if (child.isArray()) return child;
            if (child.isObject()) {
                JsonNode nested = firstArray(child, keys);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode n = node.get(k);
            if (n != null && !n.isNull() && !n.asText().isBlank()) return n.asText().trim();
        }
        return null;
    }

    private String saveSnapshot(String body, String hash) {
        if (snapshotStore == null) return null;
        try {
            byte[] content = body.getBytes(StandardCharsets.UTF_8);
            return snapshotStore.save("KAOYAN_SCHOOL", props.getKaoyanDataYear(), hash, content);
        } catch (RuntimeException e) {
            log.warn("[crawler:KAOYAN] 保存原始快照失败，将继续处理当前响应: {}", e.getMessage());
            return null;
        }
    }

    private DataImportBatch beginBatch(String hash, String snapshotPath, String sourceUrl) {
        if (batchService == null) return null;
        DataImportBatch batch = batchService.createOrReuse("KAOYAN_SCHOOL", props.getKaoyanDataYear(), "sha256:" + hash, "v1");
        if (snapshotPath != null) batchService.attachSnapshot(batch.getId(), sourceUrl, snapshotPath);
        if (!"PUBLISHED".equals(batch.getStatus()) && !"APPROVED".equals(batch.getStatus())) batchService.markRunning(batch.getId());
        return batch;
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
