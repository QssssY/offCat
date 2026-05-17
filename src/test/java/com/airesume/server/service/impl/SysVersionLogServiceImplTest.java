package com.airesume.server.service.impl;

import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.mapper.SysVersionLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysVersionLogServiceImplTest {

    @Mock
    private SysVersionLogMapper sysVersionLogMapper;

    @Captor
    private ArgumentCaptor<Page<SysVersionLog>> pageCaptor;

    @Test
    void getLatestPublishedShouldReturnOnlyPublishedLogs() {
        SysVersionLogServiceImpl service = new SysVersionLogServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysVersionLogMapper);

        SysVersionLog log1 = new SysVersionLog();
        log1.setId(1L);
        log1.setVersion("2.0.0");
        log1.setStatus(1);
        log1.setPublishedAt(LocalDateTime.now());

        SysVersionLog log2 = new SysVersionLog();
        log2.setId(2L);
        log2.setVersion("1.9.0");
        log2.setStatus(1);
        log2.setPublishedAt(LocalDateTime.now().minusDays(5));

        Page<SysVersionLog> page = new Page<>(1, 5);
        page.setRecords(List.of(log1, log2));
        page.setTotal(2);
        when(sysVersionLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        List<SysVersionLog> result = service.getLatestPublished(5);
        assertEquals(2, result.size());
    }

    @Test
    void getLatestPublishedShouldReturnEmptyWhenNoPublished() {
        SysVersionLogServiceImpl service = new SysVersionLogServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysVersionLogMapper);

        Page<SysVersionLog> page = new Page<>(1, 5);
        page.setRecords(List.of());
        page.setTotal(0);
        when(sysVersionLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        List<SysVersionLog> result = service.getLatestPublished(5);
        assertTrue(result.isEmpty());
    }
}
