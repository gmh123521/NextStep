package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
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

    @Test
    void approvesOnlySucceededBatch() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(8L);
        batch.setStatus("SUCCEEDED");
        when(mapper.selectById(8L)).thenReturn(batch);

        DataImportBatchService service = new DataImportBatchService(mapper);
        service.approve(8L);

        assertEquals("APPROVED", batch.getStatus());
        verify(mapper).updateById(batch);
    }

    @Test
    void rejectsPublishingUnapprovedBatch() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(9L);
        batch.setStatus("SUCCEEDED");
        when(mapper.selectById(9L)).thenReturn(batch);

        DataImportBatchService service = new DataImportBatchService(mapper);

        assertThrows(RuntimeException.class, () -> service.publish(9L));
        verify(mapper, never()).updateById(any());
    }

    @Test
    void publishesApprovedBatchAndRollsBackPublishedBatch() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(10L);
        batch.setStatus("APPROVED");
        when(mapper.selectById(10L)).thenReturn(batch);

        DataImportBatchService service = new DataImportBatchService(mapper);
        service.publish(10L);
        assertEquals("PUBLISHED", batch.getStatus());
        verify(mapper).updateById(batch);

        batch.setStatus("PUBLISHED");
        service.rollback(10L, "数据质量复核");
        assertEquals("ROLLED_BACK", batch.getStatus());
    }

    @Test
    void reparsesOnlyBatchWithSnapshot() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(11L);
        batch.setStatus("FAILED");
        batch.setSnapshotPath("snapshots/11.json");
        batch.setFailedCount(2);
        when(mapper.selectById(11L)).thenReturn(batch);

        DataImportBatchService service = new DataImportBatchService(mapper);
        service.reparse(11L);

        assertEquals("PENDING", batch.getStatus());
        assertEquals(0, batch.getFailedCount());
        verify(mapper).updateById(batch);
    }

    @Test
    void pagesWithoutOptionalFilters() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        when(mapper.selectPage(any(Page.class), any())).thenReturn(Page.of(1, 10));

        PageResult<DataImportBatch> result = new DataImportBatchService(mapper).page(1, 10, null, null, null);

        assertEquals(0, result.getTotal());
        verify(mapper).selectPage(any(Page.class), any());
    }
}
