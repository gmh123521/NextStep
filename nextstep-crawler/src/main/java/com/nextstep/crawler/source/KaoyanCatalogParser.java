package com.nextstep.crawler.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextstep.crawler.dto.KaoyanCatalogRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** 将公开考研专业目录响应转换为统一记录，不执行数据库写入。 */
@Component
public class KaoyanCatalogParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParseResult parse(String body) {
        if (body == null || body.isBlank()) return new ParseResult(List.of(), List.of());

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            return new ParseResult(List.of(), List.of("响应不是有效 JSON"));
        }

        JsonNode rows = firstArray(root);
        if (rows == null) return new ParseResult(List.of(), List.of("JSON 中未找到专业目录列表节点"));

        List<KaoyanCatalogRecord> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int index = 0;
        for (JsonNode row : rows) {
            index++;
            String schoolCode = text(row, "schoolCode", "dwdm", "yxdm", "code");
            String schoolName = text(row, "schoolName", "dwmc", "yxmc", "name");
            String majorCode = text(row, "majorCode", "major_code", "zydm");
            String majorName = text(row, "majorName", "major_name", "zymc");
            if (schoolCode == null || schoolName == null || majorCode == null || majorName == null) {
                errors.add("第 " + index + " 条缺少院校代码或专业代码/名称");
                continue;
            }
            records.add(new KaoyanCatalogRecord(
                    schoolCode,
                    schoolName,
                    text(row, "province", "ssmc", "sf"),
                    text(row, "city", "cs"),
                    majorCode,
                    majorName,
                    text(row, "category", "xkml", "discipline"),
                    normalizeDegree(text(row, "degreeType", "degree_type", "degree")),
                    subjects(row)
            ));
        }
        return new ParseResult(records, errors);
    }

    private JsonNode firstArray(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) return node;
        if (!node.isObject()) return null;
        for (String key : List.of("data", "rows", "list", "yxList", "ssList", "majors")) {
            JsonNode child = node.get(key);
            if (child != null) {
                JsonNode result = firstArray(child);
                if (result != null) return result;
            }
        }
        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            JsonNode result = firstArray(children.next());
            if (result != null) return result;
        }
        return null;
    }

    private List<String> subjects(JsonNode row) {
        JsonNode value = first(row, "examSubjects", "exam_subjects", "kskm", "考试科目");
        if (value == null || value.isNull()) return List.of();
        if (value.isArray()) {
            List<String> result = new ArrayList<>();
            value.forEach(item -> {
                String text = clean(item.asText());
                if (text != null) result.add(text);
            });
            return List.copyOf(result);
        }
        String text = clean(value.asText());
        if (text == null) return List.of();
        return List.of(text.split("[，,;；|]")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private String normalizeDegree(String value) {
        if (value == null) return "ACADEMIC";
        String normalized = value.toUpperCase(Locale.ROOT);
        return normalized.contains("PROFESSIONAL") || value.contains("专硕") || value.contains("专业学位")
                ? "PROFESSIONAL" : "ACADEMIC";
    }

    private String text(JsonNode node, String... keys) {
        JsonNode value = first(node, keys);
        return value == null ? null : clean(value.asText());
    }

    private JsonNode first(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isBlank() ? null : result;
    }

    public record ParseResult(List<KaoyanCatalogRecord> records, List<String> errors) {
    }
}
