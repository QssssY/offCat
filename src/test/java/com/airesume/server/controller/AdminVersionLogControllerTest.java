package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.VersionLogCreateRequest;
import com.airesume.server.dto.admin.VersionLogResponse;
import com.airesume.server.dto.admin.VersionLogUpdateRequest;
import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysVersionLog.class);
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

        Page<SysVersionLog> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(log));
        when(sysVersionLogService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getVersionLogList(
                1, 20, null, null, null, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<VersionLogResponse> records = (List<VersionLogResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("2.0.0", records.get(0).getVersion());
        assertEquals(1L, result.getData().get("total"));
    }

    @Test
    void getVersionLogListShouldApplyFiltersAndClampPageSize() {
        Page<SysVersionLog> pageResult = new Page<>(1, 100, 0);
        pageResult.setRecords(List.of());
        when(sysVersionLogService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getVersionLogList(
                0, 200, "major", 1, " 发布 ", authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        ArgumentCaptor<Page<SysVersionLog>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper<SysVersionLog>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(sysVersionLogService).page(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(100L, pageCaptor.getValue().getSize());

        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();
        assertEquals(100L, result.getData().get("size"));
        assertEquals(1L, result.getData().get("page"));
        assertEquals(0L, result.getData().get("total"));
        assertEquals(true, sqlSegment.contains("type"));
        assertEquals(true, sqlSegment.contains("status"));
        assertEquals(true, sqlSegment.contains("title"));
        assertEquals(true, sqlSegment.contains("content"));
        assertEquals(true, sqlSegment.contains("version"));
        assertEquals(true, sqlSegment.contains("create_time"));
    }

    @Test
    void getVersionLogListShouldRejectUnsupportedFilters() {
        Result<Map<String, Object>> typeResult = controller.getVersionLogList(
                1, 20, "hotfix", null, null, authentication);
        Result<Map<String, Object>> statusResult = controller.getVersionLogList(
                1, 20, null, 3, null, authentication);

        assertEquals(500, typeResult.getCode());
        assertEquals(500, statusResult.getCode());
        verify(sysVersionLogService, never()).page(any(Page.class), any(Wrapper.class));
    }

    @Test
    void createVersionLogShouldReturnNewId() {
        VersionLogCreateRequest request = new VersionLogCreateRequest();
        request.setVersion("2.1.0");
        request.setTitle("New feature");
        request.setContent("Feature notes");
        request.setType("minor");

        LambdaQueryChainWrapper<SysVersionLog> wrapper = mockVersionLogQuery();
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

        LambdaQueryChainWrapper<SysVersionLog> wrapper = mockVersionLogQuery();
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

    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<SysVersionLog> mockVersionLogQuery() {
        LambdaQueryChainWrapper<SysVersionLog> wrapper = mock(LambdaQueryChainWrapper.class);
        when(sysVersionLogService.lambdaQuery()).thenReturn(wrapper);
        return wrapper;
    }
}
