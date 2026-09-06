package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.entity.DataRawRecord;
import com.nextstep.crawler.mapper.DataImportBatchMapper;
import com.nextstep.crawler.mapper.DataRawRecordMapper;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

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

    @Test
    void savesRawImportPage() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataRawRecordMapper rawMapper = mock(DataRawRecordMapper.class);

        new DataImportBatchService(mapper, rawMapper)
                .saveRawRecord(12L, 1, "{\"msg\":{}}", "[]", "sha256:page", "SUCCESS", null);

        ArgumentCaptor<DataRawRecord> captor = ArgumentCaptor.forClass(DataRawRecord.class);
        verify(rawMapper).insert(captor.capture());
        assertEquals("[]", captor.getValue().getNormalizedPayload());
    }

    @Test
    void clearsRawImportPagesBeforeReplay() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataRawRecordMapper rawMapper = mock(DataRawRecordMapper.class);

        new DataImportBatchService(mapper, rawMapper).clearRawRecords(12L);

        verify(rawMapper).delete(any());
    }

    @Test
    void publishesCatalogBatchFromNormalizedRawRecords() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataRawRecordMapper rawMapper = mock(DataRawRecordMapper.class);
        KaoyanPublishService publisher = mock(KaoyanPublishService.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(13L);
        batch.setSourceCode("KAOYAN_CATALOG");
        batch.setStatus("APPROVED");
        when(mapper.selectById(13L)).thenReturn(batch);

        DataRawRecord raw = new DataRawRecord();
        raw.setBatchId(13L);
        raw.setRecordNo(1);
        raw.setParseStatus("SUCCESS");
        raw.setNormalizedPayload("[{\"schoolCode\":\"10001\",\"schoolName\":\"示例大学\",\"majorCode\":\"081000\",\"majorName\":\"软件工程\",\"degreeType\":\"ACADEMIC\",\"year\":2026}]");
        when(rawMapper.selectList(any())).thenReturn(java.util.List.of(raw));

        DataImportBatchService service = new DataImportBatchService(mapper, rawMapper, publisher);
        service.publish(13L);

        verify(publisher).publish(any(), eq(java.util.List.of()));
        assertEquals("PUBLISHED", batch.getStatus());
        verify(mapper).updateById(batch);
    }

    @Test
    void rejectsPublishingCatalogBatchWithoutSchoolAssociation() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataRawRecordMapper rawMapper = mock(DataRawRecordMapper.class);
        KaoyanPublishService publisher = mock(KaoyanPublishService.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(14L);
        batch.setSourceCode("KAOYAN_CATALOG");
        batch.setStatus("APPROVED");
        when(mapper.selectById(14L)).thenReturn(batch);

        DataRawRecord raw = new DataRawRecord();
        raw.setBatchId(14L);
        raw.setRecordNo(1);
        raw.setParseStatus("SUCCESS");
        raw.setNormalizedPayload("[{\"schoolCode\":null,\"schoolName\":null,\"majorCode\":\"081000\",\"majorName\":\"软件工程\",\"degreeType\":\"ACADEMIC\",\"year\":2026}]");
        when(rawMapper.selectList(any())).thenReturn(java.util.List.of(raw));

        DataImportBatchService service = new DataImportBatchService(mapper, rawMapper, publisher);

        assertThrows(RuntimeException.class, () -> service.publish(14L));
        verify(publisher, never()).publish(any(), any());
        verify(mapper, never()).updateById(batch);
    }

    @Test
    void rejectsPublishingCatalogBatchWhenStandardizedPayloadIsInvalid() {
        DataImportBatchMapper mapper = mock(DataImportBatchMapper.class);
        DataRawRecordMapper rawMapper = mock(DataRawRecordMapper.class);
        KaoyanPublishService publisher = mock(KaoyanPublishService.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(15L);
        batch.setSourceCode("KAOYAN_CATALOG");
        batch.setStatus("APPROVED");
        when(mapper.selectById(15L)).thenReturn(batch);

        DataRawRecord raw = new DataRawRecord();
        raw.setBatchId(15L);
        raw.setRecordNo(1);
        raw.setParseStatus("SUCCESS");
        raw.setNormalizedPayload("not-json");
        when(rawMapper.selectList(any())).thenReturn(java.util.List.of(raw));

        DataImportBatchService service = new DataImportBatchService(mapper, rawMapper, publisher);

        assertThrows(RuntimeException.class, () -> service.publish(15L));
        verify(publisher, never()).publish(any(), any());
        verify(mapper, never()).updateById(batch);
    }
}
