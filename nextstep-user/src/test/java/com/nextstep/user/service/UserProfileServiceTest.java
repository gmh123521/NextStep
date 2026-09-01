package com.nextstep.user.service;

import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserProfileServiceTest {

    @Test
    void completeSaveCopiesExplicitNullsToClearOldValues() {
        UserProfile existing = new UserProfile();
        existing.setCurrentSchool("示例大学");
        existing.setCurrentMajor("计算机科学与技术");
        existing.setEnglishLevel("CET6");
        existing.setEnglishScore(520);

        UserProfileRequest request = new UserProfileRequest();
        request.setCurrentSchool("新大学");
        request.setCurrentMajor(null);
        request.setEnglishLevel(null);
        request.setEnglishScore(null);

        UserProfileService.copyAllFields(request, existing);

        assertEquals("新大学", existing.getCurrentSchool());
        assertNull(existing.getCurrentMajor());
        assertNull(existing.getEnglishLevel());
        assertNull(existing.getEnglishScore());
    }

    @Test
    void partialSaveKeepsUnspecifiedOldValues() {
        UserProfile existing = new UserProfile();
        existing.setCurrentMajor("计算机科学与技术");
        existing.setEnglishLevel("CET6");

        UserProfileRequest request = new UserProfileRequest();
        request.setCurrentSchool("新大学");

        UserProfileService.copyNonNullFields(request, existing);

        assertEquals("新大学", existing.getCurrentSchool());
        assertEquals("计算机科学与技术", existing.getCurrentMajor());
        assertEquals("CET6", existing.getEnglishLevel());
    }
}
