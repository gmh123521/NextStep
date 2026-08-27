package com.nextstep.crawler.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.crawler.config.CrawlerProperties;
import com.nextstep.crawler.dto.CrawlResult;
import com.nextstep.crawler.fetch.HttpFetcher;
import com.nextstep.crawler.mapper.SchoolUpsertMapper;
import com.nextstep.data.school.entity.School;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
public class KaoyanCrawler implements SourceCrawler {

    private final CrawlerProperties props;
    private final HttpFetcher fetcher;
    private final SchoolUpsertMapper schoolUpsertMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String source() {
        return "KAOYAN";
    }

    @Override
    public CrawlResult crawl() {
        CrawlResult result = new CrawlResult();
        String body = fetcher.get(props.getKaoyanUrl());
        if (body == null || body.isBlank()) {
            log.warn("[crawler:KAOYAN] 响应为空");
            return result;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("[crawler:KAOYAN] 响应非 JSON，跳过: {}", e.getMessage());
            return result;
        }

        // 兼容常见结构：顶层数组，或 {data:[...]} / {ssdm:[...]}
        JsonNode list = firstArray(root, "data", "list", "yxList", "ssList");
        if (list == null || !list.isArray()) {
            log.warn("[crawler:KAOYAN] 未找到院校数组节点");
            return result;
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
        log.info("[crawler:KAOYAN] {}", result.summary());
        return result;
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
        // 递归找第一个数组子节点
        Iterator<JsonNode> it = root.elements();
        while (it.hasNext()) {
            JsonNode child = it.next();
            if (child.isArray()) return child;
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
}
