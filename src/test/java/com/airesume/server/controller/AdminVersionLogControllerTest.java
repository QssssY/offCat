package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.VersionLogCreateRequest;
import com.airesume.server.dto.admin.VersionLogResponse;
import com.airesume.server.dto.admin.VersionLogUpdateRequest;
import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminVersionLogControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private SysVersionLogService sysVersionLogService;
    @Mock private Authentication authentication;

    private AdminVersionLogController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminVersionLogController(sysVersionLogService);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getVersionLogListShouldReturnPagedRecords() {
        SysVersionLog log = new SysVersionLog();
        log.setId(100L);
        log.setVersion("2.0.0");
        log.setTitle("Major release");
        log.setContent("Release notes");
        log.setType("major");
        log.setStatus(1);
        log.setPublishedAt(LocalDateTime.now());

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysVersionLog> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        Page<SysVersionLog> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(log));
        when(sysVersionLogService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).orderByDesc(any(SFunction.class));
        doReturn(pageResult).when(wrapper).page(any(Page.class));

        Result<Map<String, Object>> result = controller.getVersionLogList(1, 20, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<VersionLogResponse> records = (List<VersionLogResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("2.0.0", records.get(0).getVersion());
        assertEquals(1, result.getData().get("total"));
    }

    @Test
    void createVersionLogShouldReturnNewId() {
        VersionLogCreateRequest request = new VersionLogCreateRequest();
        request.setVersion("2.1.0");
        request.setTitle("New feature");
        request.setContent("Feature notes");
        request.setType("minor");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysVersionLog> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(sysVersionLogService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(0L);
        when(sysVersionLogService.save(any(SysVersionLog.class))).thenAnswer(invocation -> {
            SysVersionLog saved = invocation.getArgument(0);
            saved.setId(200L);
            return true;
        });

        Result<Long> result = controller.createVersionLog(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(200L, result.getData());
    }

    @Test
    void createVersionLogShouldThrowWhenVersionExists() {
        VersionLogCreateRequest request = new VersionLogCreateRequest();
        request.setVersion("2.0.0");
        request.setTitle("Duplicate");
        request.setContent("Content");
        request.setType("minor");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysVersionLog> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(sysVersionLogService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.createVersionLog(request, authentication));
    }

    @Test
    void updateVersionLogShouldSucceed() {
        VersionLogUpdateRequest request = new VersionLogUpdateRequest();
        request.setId(100L);
        request.setTitle("Updated title");

        SysVersionLog existingLog = new SysVersionLog();
        existingLog.setId(100L);
        existingLog.setVersion("2.0.0");
        when(sysVersionLogService.getById(100L)).thenReturn(existingLog);

        Result<Void> result = controller.updateVersionLog(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysVersionLogService).updateById(existingLog);
    }

    @Test
    void updateVersionLogShouldThrowWhenNotFound() {
        VersionLogUpdateRequest request = new VersionLogUpdateRequest();
        request.setId(999L);
        when(sysVersionLogService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.updateVersionLog(request, authentication));
    }

    @Test
    void publishVersionLogShouldSucceed() {
        SysVersionLog log = new SysVersionLog();
        log.setId(100L);
        log.setStatus(0);
        when(sysVersionLogService.getById(100L)).thenReturn(log);

        Result<Void> result = controller.publishVersionLog(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(1, log.getStatus());
        verify(sysVersionLogService).updateById(log);
    }

    @Test
    void publishVersionLogShouldErrorWhenAlreadyPublished() {
        SysVersionLog log = new SysVersionLog();
        log.setId(100L);
        log.setStatus(1);
        when(sysVersionLogService.getById(100L)).thenReturn(log);

        Result<Void> result = controller.publishVersionLog(100L, authentication);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteVersionLogShouldRemove() {
        Result<Void> result = controller.deleteVersionLog(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysVersionLogService).removeById(100L);
    }

    @Test
    void publishVersionLogsBatchShouldPublishDraftsOnly() {
        SysVersionLog draft = new SysVersionLog();
        draft.setId(100L);
        draft.setStatus(0);
        SysVersionLog published = new SysVersionLog();
        published.setId(200L);
        published.setStatus(1);
        when(sysVersionLogService.listByIds(List.of(100L, 200L))).thenReturn(List.of(draft, published));

        Result<Void> result = controller.publishVersionLogsBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(1, draft.getStatus());
        verify(sysVersionLogService).updateById(draft);
        verify(sysVersionLogService, never()).updateById(published);
    }

    @Test
    void publishVersionLogsBatchShouldRejectEmptyIds() {
        assertThrows(BusinessException.class, () -> controller.publishVersionLogsBatch(List.of(), authentication));
    }

    @Test
    void deleteVersionLogsBatchShouldRemoveIds() {
        Result<Void> result = controller.deleteVersionLogsBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysVersionLogService).removeByIds(List.of(100L, 200L));
    }
}
