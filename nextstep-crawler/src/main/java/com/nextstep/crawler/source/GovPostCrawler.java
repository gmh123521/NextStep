package com.nextstep.crawler.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.mapper.GovPostUpsertMapper;
import com.nextstep.crawler.service.DataImportBatchService;
import com.nextstep.crawler.service.DataSourceService;
import com.nextstep.crawler.service.RawSnapshotStore;
import com.nextstep.data.gov.entity.GovPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 * 国家公务员局（scs.gov.cn）国考职位表采集。
 *
 * 国考职位表以结构化数据（JSON / 可解析表格）公开，本采集器：
 *   1. WebClient 拉取职位数据（UA 伪装 + 限速）
 *   2. Jackson 解析出职位记录
 *   3. INSERT IGNORE 写入 ns_gov_post，
 *      按唯一键 (year, exam_type, dept_name, post_code) 去重
 *
 * 字段名以线上响应为准，缺失字段安全降级为 null。
 */
@Slf4j
@Component
public class GovPostCrawler implements SourceCrawler {

    private final CrawlerProperties props;
    private final HttpFetcher fetcher;
    private final GovPostUpsertMapper govPostUpsertMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RawSnapshotStore snapshotStore;
    private final DataImportBatchService batchService;
    private final DataSourceService dataSourceService;

    public GovPostCrawler(CrawlerProperties props, HttpFetcher fetcher, GovPostUpsertMapper govPostUpsertMapper) {
        this(props, fetcher, govPostUpsertMapper, null, null, null);
    }

    public GovPostCrawler(CrawlerProperties props, HttpFetcher fetcher, GovPostUpsertMapper govPostUpsertMapper,
                          RawSnapshotStore snapshotStore, DataImportBatchService batchService) {
        this(props, fetcher, govPostUpsertMapper, snapshotStore, batchService, null);
    }

    @Autowired
    public GovPostCrawler(CrawlerProperties props, HttpFetcher fetcher, GovPostUpsertMapper govPostUpsertMapper,
                          RawSnapshotStore snapshotStore, DataImportBatchService batchService,
                          DataSourceService dataSourceService) {
        this.props = props;
        this.fetcher = fetcher;
        this.govPostUpsertMapper = govPostUpsertMapper;
        this.snapshotStore = snapshotStore;
        this.batchService = batchService;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public String source() {
        return "GOV_POST";
    }

    @Override
    public CrawlResult crawl() {
        CrawlResult result = new CrawlResult();
        String sourceUrl = dataSourceService == null ? props.getGovPostUrl()
                : dataSourceService.resolveUrl("GOV_POST", props.getGovPostUrl());
        String body = fetcher.get(sourceUrl);
        if (body == null || body.isBlank()) {
            throw new BizException("国考岗位数据响应为空");
        }

        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        String hash = sha256(content);
        String snapshotPath = saveSnapshot(content, hash);
        DataImportBatch batch = beginBatch(hash, snapshotPath, sourceUrl);
        try {
            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (Exception e) {
                throw new BizException("国考岗位数据响应格式无效");
            }

        JsonNode list = firstArray(root, "data", "list", "rows", "positions");
            if (list == null || !list.isArray()) {
                throw new BizException("国考岗位数据缺少列表节点");
            }

            for (JsonNode node : list) {
            if (result.getFetched() >= props.getMaxItems()) {
                log.info("[crawler:GOV_POST] 达到单次上限 {} 条，停止", props.getMaxItems());
                break;
            }
            result.addFetched();

            GovPost post = toPost(node);
            if (post == null || post.getYear() == null || post.getDeptName() == null) {
                result.addSkipped();
                continue;
            }
            int affected = govPostUpsertMapper.insertIgnore(post);
            if (affected > 0) result.addInserted();
            else result.addSkipped();
            }
            if (batch != null) batchService.markSucceeded(batch.getId(), result.getFetched(), result.getInserted(), result.getSkipped(), 0);
            log.info("[crawler:GOV_POST] {}", result.summary());
            return result;
        } catch (RuntimeException e) {
            if (batch != null) batchService.markFailed(batch.getId(), e.getMessage());
            throw e;
        }
    }

    private String saveSnapshot(byte[] content, String hash) {
        if (snapshotStore == null) return null;
        try {
            return snapshotStore.save("GOV_POST", props.getGovPostDataYear(), hash, content);
        } catch (RuntimeException e) {
            log.warn("[crawler:GOV_POST] 保存原始快照失败，将继续处理当前响应: {}", e.getMessage());
            return null;
        }
    }

    private DataImportBatch beginBatch(String hash, String snapshotPath, String sourceUrl) {
        if (batchService == null) return null;
        DataImportBatch batch = batchService.createOrReuse("GOV_POST", props.getGovPostDataYear(), "sha256:" + hash, "v1");
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

    private GovPost toPost(JsonNode node) {
        GovPost p = new GovPost();
        p.setYear(intVal(node, "year", "nf", "ksnd"));
        p.setExamType(defaultText(text(node, "examType", "kslx"), "NATIONAL"));
        p.setProvince(text(node, "province", "sf", "gzdd"));
        p.setDeptName(text(node, "deptName", "bmmc", "zsjg"));
        p.setPostCode(text(node, "postCode", "zwdm", "zwbm"));
        p.setPostName(text(node, "postName", "zwmc"));
        p.setRegion(text(node, "region", "dq", "gzdd"));
        p.setDegreeRequired(text(node, "degree", "xl", "xlyq"));
        p.setMajorRequired(text(node, "major", "zy", "zyyq"));
        p.setPolitical(text(node, "political", "zzmm"));
        p.setExtraRequired(text(node, "extra", "qtyq", "bz"));
        return p;
    }

    private JsonNode firstArray(JsonNode root, String... keys) {
        if (root.isArray()) return root;
        for (String k : keys) {
            JsonNode n = root.get(k);
            if (n != null && n.isArray()) return n;
        }
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

    private Integer intVal(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode n = node.get(k);
            if (n != null && !n.isNull()) {
                if (n.isNumber()) return n.asInt();
                try {
                    return Integer.parseInt(n.asText().trim());
                } catch (NumberFormatException ignored) {
                    // 尝试从形如 "2024年" 中提取数字
                    String digits = n.asText().replaceAll("\\D", "");
                    if (!digits.isBlank()) return Integer.parseInt(digits.substring(0, Math.min(4, digits.length())));
                }
            }
        }
        return null;
    }

    private String defaultText(String v, String dft) {
        return (v == null || v.isBlank()) ? dft : v;
    }
}
