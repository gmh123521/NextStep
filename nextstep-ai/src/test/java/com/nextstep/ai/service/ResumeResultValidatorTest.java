package com.nextstep.ai.service;

import com.nextstep.ai.dto.ResumeExtractResult;
import com.nextstep.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumeResultValidatorTest {

    private final ResumeResultValidator validator = new ResumeResultValidator();

    @Test
    void normalizesValidProfileAndExperienceFields() {
        ResumeExtractResult result = validResult();
        result.setSchoolLevel(" 985 ");
        result.setEnglishLevel(" cet6 ");
        result.getExperiences().get(0).setType(" project ");
        result.getExperiences().get(0).setTitle("  校园交易平台  ");
        result.getExperiences().get(0).setRole("   ");

        validator.validateAndNormalize(result);

        assertEquals("985", result.getSchoolLevel());
        assertEquals("CET6", result.getEnglishLevel());
        assertEquals("PROJECT", result.getExperiences().get(0).getType());
        assertEquals("校园交易平台", result.getExperiences().get(0).getTitle());
        assertNull(result.getExperiences().get(0).getRole());
    }

    @Test
    void rejectsInvalidProfileEnum() {
        ResumeExtractResult result = validResult();
        result.setDegreeType("POSTGRADUATE");

        BizException error = assertThrows(BizException.class,
                () -> validator.validateAndNormalize(result));

        assertEquals("学历类型无效：POSTGRADUATE", error.getMessage());
    }

    @Test
    void rejectsGpaAboveSelectedScale() {
        ResumeExtractResult result = validResult();
        result.setGpaScale(4);
        result.setGpa(4.5);

        BizException error = assertThrows(BizException.class,
                () -> validator.validateAndNormalize(result));

        assertEquals("GPA 不能超过 GPA 满分 4", error.getMessage());
    }

    @Test
    void acceptsFullScoreOnHundredPointScale() {
        ResumeExtractResult result = validResult();
        result.setGpaScale(100);
        result.setGpa(100.0);

        validator.validateAndNormalize(result);

        assertEquals(100.0, result.getGpa());
    }

    @Test
    void rejectsInvalidExperienceDate() {
        ResumeExtractResult result = validResult();
        result.getExperiences().get(0).setStartDate("2026-13");

        BizException error = assertThrows(BizException.class,
                () -> validator.validateAndNormalize(result));

        assertEquals("第 1 条经历开始日期格式应为 YYYY 或 YYYY-MM", error.getMessage());
    }

    @Test
    void rejectsExperienceTitleBeyondDatabaseLimit() {
        ResumeExtractResult result = validResult();
        result.getExperiences().get(0).setTitle("项".repeat(256));

        BizException error = assertThrows(BizException.class,
                () -> validator.validateAndNormalize(result));

        assertEquals("第 1 条经历标题不能超过 255 个字符", error.getMessage());
    }

    private ResumeExtractResult validResult() {
        ResumeExtractResult result = new ResumeExtractResult();
        result.setCurrentSchool("示例大学");
        result.setSchoolLevel("REGULAR");
        result.setCurrentMajor("计算机科学与技术");
        result.setDegreeType("BACHELOR");
        result.setGradeYear(4);
        result.setGpa(3.6);
        result.setGpaScale(4);
        result.setEnglishLevel("CET6");
        result.setEnglishScore(520);

        ResumeExtractResult.ExperienceItem item = new ResumeExtractResult.ExperienceItem();
        item.setType("PROJECT");
        item.setTitle("校园交易平台");
        item.setRole("后端开发");
        item.setStartDate("2025-03");
        item.setEndDate("2025-06");
        item.setDescription("负责接口设计与数据库优化");
        result.setExperiences(List.of(item));
        return result;
    }
}
