package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.VersionLogResponse;
import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionLogControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private SysVersionLogService sysVersionLogService;

    private VersionLogController controller;

    @BeforeEach
    void setUp() {
        controller = new VersionLogController(sysVersionLogService);
    }

    @Test
    void getLatestVersionLogsShouldReturnLimitedPublished() {
        SysVersionLog log1 = new SysVersionLog();
        log1.setId(1L);
        log1.setVersion("2.1.0");
        log1.setTitle("新增功能");
        log1.setContent("新功能上线");
        log1.setType("minor");
        log1.setStatus(1);
        log1.setPublishedAt(LocalDateTime.now());
        log1.setCreateTime(LocalDateTime.now());

        SysVersionLog log2 = new SysVersionLog();
        log2.setId(2L);
        log2.setVersion("2.0.0");
        log2.setTitle("大版本更新");
        log2.setContent("重大更新");
        log2.setType("major");
        log2.setStatus(1);
        log2.setPublishedAt(LocalDateTime.now().minusDays(7));
        log2.setCreateTime(LocalDateTime.now().minusDays(7));

        when(sysVersionLogService.getLatestPublished(3)).thenReturn(List.of(log1, log2));

        Result<List<VersionLogResponse>> result = controller.getLatestVersionLogs(3);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("2.1.0", result.getData().get(0).getVersion());
        assertEquals("小版本", result.getData().get(0).getTypeDesc());
        assertEquals("已发布", result.getData().get(0).getStatusDesc());
    }

    @Test
    void getVersionLogsShouldReturnPublishedPage() {
        SysVersionLog log = new SysVersionLog();
        log.setId(1L);
        log.setVersion("2.2.0");
        log.setTitle("分页动态");
        log.setContent("更多动态分页展示");
        log.setType("minor");
        log.setStatus(1);
        log.setPublishedAt(LocalDateTime.now());
        log.setCreateTime(LocalDateTime.now());

        Page<SysVersionLog> pageResult = new Page<>(2, 10, 21);
        pageResult.setRecords(List.of(log));
        when(sysVersionLogService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getVersionLogs(2, 10);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(21L, result.getData().get("total"));
        assertEquals(2L, result.getData().get("page"));
        assertEquals(10L, result.getData().get("size"));
        List<VersionLogResponse> records = (List<VersionLogResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("2.2.0", records.get(0).getVersion());
    }

    @Test
    void getVersionLogsShouldClampPageAndSize() {
        Page<SysVersionLog> pageResult = new Page<>(1, 50, 0);
        pageResult.setRecords(List.of());
        when(sysVersionLogService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getVersionLogs(0, 200);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(50L, result.getData().get("size"));
        verify(sysVersionLogService).page(any(Page.class), any(Wrapper.class));
    }

    @Test
    void getLatestVersionLogsShouldUseDefaultLimit() {
        when(sysVersionLogService.getLatestPublished(5)).thenReturn(List.of());

        Result<List<VersionLogResponse>> result = controller.getLatestVersionLogs(5);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, result.getData().size());
    }

    @Test
    void getLatestVersionLogsShouldClampTooLargeLimit() {
        when(sysVersionLogService.getLatestPublished(20)).thenReturn(List.of());

        Result<List<VersionLogResponse>> result = controller.getLatestVersionLogs(100);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, result.getData().size());
        verify(sysVersionLogService).getLatestPublished(20);
    }

    @Test
    void getLatestVersionLogsShouldClampNonPositiveLimit() {
        when(sysVersionLogService.getLatestPublished(1)).thenReturn(List.of());

        Result<List<VersionLogResponse>> result = controller.getLatestVersionLogs(0);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, result.getData().size());
        verify(sysVersionLogService).getLatestPublished(1);
    }
}
