package com.nextstep.crawler.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KaoyanCatalogParserTest {

    private final KaoyanCatalogParser parser = new KaoyanCatalogParser();

    @Test
    void parsesTopLevelCatalogRecordsAndNormalizesDegreeType() {
        KaoyanCatalogParser.ParseResult result = parser.parse("""
                [{"schoolCode":"10001","schoolName":" 北京大学 ","province":"北京市","city":"北京",
                  "majorCode":"081000","majorName":"计算机科学与技术","category":"工学","year":2026,
                  "degreeType":"专硕","examSubjects":["101","204","302","408"]}]
                """);

        assertEquals(1, result.records().size());
        assertEquals("10001", result.records().get(0).schoolCode());
        assertEquals("北京大学", result.records().get(0).schoolName());
        assertEquals("PROFESSIONAL", result.records().get(0).degreeType());
        assertEquals(2026, result.records().get(0).year());
        assertEquals(4, result.records().get(0).examSubjects().size());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parsesNestedRowsAndReportsMissingBusinessKeys() {
        KaoyanCatalogParser.ParseResult result = parser.parse("""
                {"data":{"rows":[
                  {"dwdm":"10002","dwmc":"清华大学","zydm":"081000","zymc":"软件工程","degree":"学硕","dataYear":2026},
                  {"dwmc":"缺少代码","zymc":"无效专业"}
                ]}}
                """);

        assertEquals(1, result.records().size());
        assertEquals("ACADEMIC", result.records().get(0).degreeType());
        assertEquals(2026, result.records().get(0).year());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("院校代码或专业代码"));
    }

    @Test
    void parsesChsiRealResponseUnderMsgList() {
        KaoyanCatalogParser.ParseResult result = parser.parse("""
                {
                  "zsmlcxModel":{"zydm":"081200","zymc":"计算机科学与技术","start":"0"},
                  "msg":{"curPage":1,"nextPageAvailable":false,"pageCount":10,"totalCount":1,
                    "list":[{"zydm":"081200","zymc":"计算机科学与技术","mldm":"08",
                      "mlmc":"工学","yjxkdm":"0812","yjxkmc":"计算机科学与技术",
                      "xwlx":"xs","xwlxmc":"学术学位"}]},
                  "flag":true,"invokeStatus":"SUCCESS"
                }
                """);

        assertEquals(1, result.records().size());
        assertEquals("081200", result.records().get(0).majorCode());
        assertEquals("计算机科学与技术", result.records().get(0).majorName());
        assertEquals("工学", result.records().get(0).category());
        assertEquals("ACADEMIC", result.records().get(0).degreeType());
        assertTrue(result.errors().get(0).contains("待关联院校"));
    }

    @Test
    void rejectsBlankOrInvalidPayload() {
        assertTrue(parser.parse(" ").records().isEmpty());
        assertTrue(parser.parse("not-json").errors().get(0).contains("JSON"));
    }
}
