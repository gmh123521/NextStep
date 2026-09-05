package com.nextstep.crawler.service;

import com.nextstep.crawler.entity.DataSource;
import com.nextstep.crawler.mapper.DataSourceMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DataSourceServiceTest {

    @Test
    void updatesUrlEnabledAndParserVersion() {
        DataSourceMapper mapper = mock(DataSourceMapper.class);
        DataSource source = new DataSource();
        source.setId(1L);
        when(mapper.selectById(1L)).thenReturn(source);

        new DataSourceService(mapper).update(1L, "https://example.test/data", 1, "v2");

        assertEquals("https://example.test/data", source.getSourceUrl());
        assertEquals(1, source.getEnabled());
        assertEquals("v2", source.getParserVersion());
        verify(mapper).updateById(source);
    }

    @Test
    void rejectsUnsafeUrlAndInvalidEnabledState() {
        DataSourceMapper mapper = mock(DataSourceMapper.class);
        DataSource source = new DataSource();
        source.setId(2L);
        when(mapper.selectById(2L)).thenReturn(source);
        DataSourceService service = new DataSourceService(mapper);

        assertThrows(RuntimeException.class, () -> service.update(2L, "javascript:alert(1)", 1, "v1"));
        assertThrows(RuntimeException.class, () -> service.update(2L, "https://example.test", 2, "v1"));
        verify(mapper, never()).updateById(any());
    }

    @Test
    void resolvesEnabledConfiguredUrlAndFallsBackWhenMissing() {
        DataSourceMapper mapper = mock(DataSourceMapper.class);
        DataSource source = new DataSource();
        source.setSourceCode("GOV_POST");
        source.setEnabled(1);
        source.setSourceUrl(" https://official.example/jobs ");
        when(mapper.selectOne(any())).thenReturn(source);
        DataSourceService service = new DataSourceService(mapper);

        assertEquals("https://official.example/jobs", service.resolveUrl("GOV_POST", "https://fallback.example"));

        source.setEnabled(0);
        assertEquals("https://fallback.example", service.resolveUrl("GOV_POST", "https://fallback.example"));
    }
}
