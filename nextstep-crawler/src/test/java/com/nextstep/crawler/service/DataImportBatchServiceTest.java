package com.nextstep.crawler.service;

import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.mapper.DataImportBatchMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataImportBatchServiceTest {

    @Test
    void reusesBatchWhenSourceYearAndHashMatch() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatch existing = new DataImportBatch();
        existing.setId(7L);
        when(mapper.selectOne(any())).thenReturn(existing);

        DataImportBatchService service = new DataImportBatchService(mapper);
        DataImportBatch result = service.createOrReuse("KAOYAN_CATALOG", 2026, "sha256:abc", "v1");

        assertEquals(7L, result.getId());
        verify(mapper, never()).insert(any());
    }

    @Test
    void rejectsInvalidBatchArguments() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatchService service = new DataImportBatchService(mapper);

        assertThrows(RuntimeException.class, () -> service.createOrReuse(" ", 2026, "sha256:abc", "v1"));
        assertThrows(RuntimeException.class, () -> service.createOrReuse("KAOYAN_CATALOG", 1999, "sha256:abc", "v1"));
        assertThrows(RuntimeException.class, () -> service.createOrReuse("KAOYAN_CATALOG", 2026, " ", "v1"));
        assertThrows(RuntimeException.class, () -> service.createOrReuse("KAOYAN_CATALOG", 2026, "sha256:abc", " "));
    }
}
