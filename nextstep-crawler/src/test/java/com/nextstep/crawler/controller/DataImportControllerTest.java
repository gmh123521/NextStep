package com.nextstep.crawler.controller;

import com.nextstep.common.core.R;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.service.DataImportBatchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DataImportControllerTest {

    @Test
    void approvedBatchDetailIsReturned() {
        DataImportBatchService service = mock(DataImportBatchService.class);
        DataImportBatch batch = new DataImportBatch();
        batch.setId(4L);
        batch.setSourceCode("KAOYAN_CATALOG");
        batch.setDataYear(2026);
        batch.setStatus("APPROVED");
        when(service.detail(4L)).thenReturn(batch);

        R<DataImportBatch> response = new DataImportController(service).detail(4L);

        assertEquals(200, response.getCode());
        assertEquals("KAOYAN_CATALOG", response.getData().getSourceCode());
        assertEquals(2026, response.getData().getDataYear());
    }

    @Test
    void approveDelegatesToService() {
        DataImportBatchService service = mock(DataImportBatchService.class);
        DataImportController controller = new DataImportController(service);

        controller.approve(12L);

        verify(service).approve(12L);
    }
}
