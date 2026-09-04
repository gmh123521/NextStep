package com.nextstep.crawler.dto;

import java.util.List;

/** 已标准化的考研院校专业目录记录，解析阶段不直接写数据库。 */
public record KaoyanCatalogRecord(
        String schoolCode,
        String schoolName,
        String province,
        String city,
        String majorCode,
        String majorName,
        String category,
        String degreeType,
        List<String> examSubjects
) {
}
