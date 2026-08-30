package com.nextstep.user.service;

import com.nextstep.common.exception.BizException;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserProfileValidatorTest {

    private final UserProfileValidator validator = new UserProfileValidator();

    @Test
    void normalizesEnumsBlankTextAndTargetPaths() {
        UserProfileRequest request = new UserProfileRequest();
        request.setCurrentSchool("  示例大学  ");
        request.setCurrentMajor("   ");
        request.setEnglishLevel(" jlpt_n1 ");
        request.setCurrentStatus(" job_hunting ");
        request.setTargetPaths(" em, PG,em ");

        validator.normalizeRequest(request);

        assertEquals("示例大学", request.getCurrentSchool());
        assertNull(request.getCurrentMajor());
        assertEquals("JLPT_N1", request.getEnglishLevel());
        assertEquals("JOB_HUNTING", request.getCurrentStatus());
        assertEquals("EM,PG", request.getTargetPaths());
    }

    @Test
    void rejectsUnknownTargetPath() {
        UserProfileRequest request = new UserProfileRequest();
        request.setTargetPaths("PG,ABROAD");

        BizException error = assertThrows(BizException.class,
                () -> validator.normalizeRequest(request));

        assertEquals("目标路径无效：ABROAD", error.getMessage());
    }

    @Test
    void rejectsGpaAboveMergedProfileScale() {
        UserProfile profile = validProfile();
        profile.setGpa(new BigDecimal("4.50"));

        BizException error = assertThrows(BizException.class,
                () -> validator.validateProfile(profile));

        assertEquals("GPA 不能超过 GPA 满分 4", error.getMessage());
    }

    @Test
    void acceptsHundredPointFullScore() {
        UserProfile profile = validProfile();
        profile.setGpaScale(100);
        profile.setGpa(new BigDecimal("100.00"));

        validator.validateProfile(profile);

        assertEquals(new BigDecimal("100.00"), profile.getGpa());
    }

    @Test
    void rejectsLanguageScoreAboveLevelMaximum() {
        UserProfile profile = validProfile();
        profile.setEnglishLevel("TOEFL");
        profile.setEnglishScore(121);

        BizException error = assertThrows(BizException.class,
                () -> validator.validateProfile(profile));

        assertEquals("TOEFL 成绩不能超过 120", error.getMessage());
    }

    @Test
    void rejectsGradeYearThatConflictsWithDegree() {
        UserProfile profile = validProfile();
        profile.setDegreeType("BACHELOR");
        profile.setGradeYear(5);

        BizException error = assertThrows(BizException.class,
                () -> validator.validateProfile(profile));

        assertEquals("本科年级必须处于 1-4 之间", error.getMessage());
    }

    @Test
    void rejectsNegativeSalaryExpectation() {
        UserProfile profile = validProfile();
        profile.setSalaryExpectation(-1);

        BizException error = assertThrows(BizException.class,
                () -> validator.validateProfile(profile));

        assertEquals("期望月薪不能为负数", error.getMessage());
    }

    private UserProfile validProfile() {
        UserProfile profile = new UserProfile();
        profile.setSchoolLevel("REGULAR");
        profile.setDegreeType("BACHELOR");
        profile.setGradeYear(4);
        profile.setGpaScale(4);
        profile.setGpa(new BigDecimal("3.60"));
        profile.setEnglishLevel("CET6");
        profile.setEnglishScore(520);
        profile.setTargetPaths("PG,EM");
        profile.setRiskAppetite(3);
        profile.setCurrentStatus("IN_SCHOOL");
        return profile;
    }
}
