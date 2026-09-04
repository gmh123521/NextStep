package com.nextstep.crawler.service;

import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.dto.KaoyanCatalogRecord;
import com.nextstep.crawler.dto.KaoyanEnrollmentRecord;
import com.nextstep.crawler.mapper.SchoolEnrollUpsertMapper;
import com.nextstep.crawler.mapper.SchoolMajorUpsertMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KaoyanPublishServiceTest {

    @Test
    void rejectsNegativeScoresAndInvalidScoreRange() {
        KaoyanPublishService service = new KaoyanPublishService(
                mock(SchoolMajorUpsertMapper.class), mock(SchoolEnrollUpsertMapper.class));

        assertThrows(BizException.class, () -> service.validate(new KaoyanEnrollmentRecord(
                "10001", "081000", 2026, 10, -1, 100, 300, 40, 40, 400, 300)));
        assertThrows(BizException.class, () -> service.validate(new KaoyanEnrollmentRecord(
                "10001", "081000", 2026, 10, 8, 100, 300, 40, 40, 200, 300)));
    }

    @Test
    void allowsOptionalMetricsToBeMissing() {
        KaoyanPublishService service = new KaoyanPublishService(
                mock(SchoolMajorUpsertMapper.class), mock(SchoolEnrollUpsertMapper.class));

        assertDoesNotThrow(() -> service.validate(new KaoyanEnrollmentRecord(
                "10001", "081000", 2026, null, null, null, null, null, null, null, null)));
    }

    @Test
    void resolvesSchoolByOfficialCodeAndPublishesCatalogAndEnrollment() {
        SchoolMajorUpsertMapper majorMapper = mock(SchoolMajorUpsertMapper.class);
        SchoolEnrollUpsertMapper enrollMapper = mock(SchoolEnrollUpsertMapper.class);
        when(majorMapper.findSchoolIdByCode("10001")).thenReturn(11L);
        when(majorMapper.upsert(any())).thenReturn(1);
        when(majorMapper.findMajorId("10001", "081000", 2026)).thenReturn(21L);
        when(enrollMapper.upsert(any())).thenReturn(1);
        KaoyanPublishService service = new KaoyanPublishService(majorMapper, enrollMapper);

        KaoyanCatalogRecord catalog = new KaoyanCatalogRecord(
                "10001", "示例大学", "北京", "北京", "081000", "软件工程", "工学", "ACADEMIC", List.of("101"), 2026);
        KaoyanEnrollmentRecord enrollment = new KaoyanEnrollmentRecord(
                "10001", "081000", 2026, 10, 8, 100, 300, 40, 40, 320, 380);

        assertDoesNotThrow(() -> service.publish(List.of(catalog), List.of(enrollment)));
        verify(majorMapper).findSchoolIdByCode(eq("10001"));
        verify(majorMapper).upsert(any());
        verify(enrollMapper).upsert(any());
    }
}
