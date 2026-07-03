package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminNotificationCreateRequest;
import com.airesume.server.dto.admin.AdminNotificationResponse;
import com.airesume.server.entity.SysAdminNotification;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.UserNotificationMapper;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.SysAdminNotificationService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private SysAdminNotificationService sysAdminNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private SysUserService sysUserService;
    @Mock private UserNotificationMapper userNotificationMapper;
    @Mock private Authentication authentication;

    private AdminNotificationController controller;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysAdminNotification.class);
        controller = new AdminNotificationController(sysAdminNotificationService, notificationService, sysUserService, userNotificationMapper);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getNotificationListShouldReturnPagedRecords() {
        SysAdminNotification notification = buildNotification(100L, 1);

        Page<SysAdminNotification> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(notification));
        when(sysAdminNotificationService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getNotificationList(1, 20, null, null, null, null, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<AdminNotificationResponse> records = (List<AdminNotificationResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("System notice", records.get(0).getTitle());
        assertEquals(1, result.getData().get("total"));
    }

    @Test
    void getNotificationListShouldApplyFilterParameters() {
        Page<SysAdminNotification> pageResult = new Page<>(1, 20, 0);
        pageResult.setRecords(List.of());
        when(sysAdminNotificationService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getNotificationList(
                1, 20, "activity", 0, "vip", "维护", authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<LambdaQueryWrapper<SysAdminNotification>> wrapperCaptor = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(sysAdminNotificationService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("type"), sqlSegment);
        assertTrue(sqlSegment.contains("status"), sqlSegment);
        assertTrue(sqlSegment.contains("target_type"), sqlSegment);
        assertTrue(sqlSegment.contains("title"), sqlSegment);
        assertTrue(sqlSegment.contains("content"), sqlSegment);
        assertTrue(sqlSegment.contains("ORDER BY"), sqlSegment);
    }

    @Test
    void getNotificationListShouldLimitPageSizeForPerformance() {
        Page<SysAdminNotification> pageResult = new Page<>(1, 100, 0);
        pageResult.setRecords(List.of());
        when(sysAdminNotificationService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getNotificationList(0, 500, null, null, null, null, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Page<SysAdminNotification>> pageCaptor = ArgumentCaptor.forClass((Class) Page.class);
        verify(sysAdminNotificationService).page(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(100, pageCaptor.getValue().getSize());
    }

    @Test
    void getNotificationListShouldRejectInvalidStatusFilter() {
        Result<Map<String, Object>> result = controller.getNotificationList(1, 20, null, 3, null, null, authentication);

        assertEquals(500, result.getCode());
        verify(sysAdminNotificationService, never()).page(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void getNotificationDetailShouldReturnDetail() {
        SysAdminNotification notification = buildNotification(100L, 1);
        notification.setType("activity");
        notification.setTargetType("vip");
        when(sysAdminNotificationService.getById(100L)).thenReturn(notification);

        Result<AdminNotificationResponse> result = controller.getNotificationDetail(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals("System notice", result.getData().getTitle());
        assertEquals("activity", result.getData().getType());
        assertEquals("vip", result.getData().getTargetType());
    }

    @Test
    void getNotificationDetailShouldReturnErrorWhenNotFound() {
        when(sysAdminNotificationService.getById(999L)).thenReturn(null);

        Result<AdminNotificationResponse> result = controller.getNotificationDetail(999L, authentication);

        assertEquals(500, result.getCode());
    }

    @Test
    void createAndSendNotificationAsDraftShouldNotBroadcast() {
        AdminNotificationCreateRequest request = buildRequest(0);
        when(sysAdminNotificationService.save(any(SysAdminNotification.class))).thenAnswer(invocation -> {
            SysAdminNotification notification = invocation.getArgument(0);
            notification.setId(200L);
            return true;
        });

        Result<Long> result = controller.createAndSendNotification(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(200L, result.getData());
        verify(notificationService, never()).createNotification(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createAndSendNotificationPublishedShouldBroadcastToAll() {
        AdminNotificationCreateRequest request = buildRequest(1);
        when(sysAdminNotificationService.save(any(SysAdminNotification.class))).thenAnswer(invocation -> {
            SysAdminNotification notification = invocation.getArgument(0);
            notification.setId(300L);
            return true;
        });

        SysUser user1 = new SysUser();
        user1.setId(10L);
        user1.setStatus(1);
        SysUser user2 = new SysUser();
        user2.setId(20L);
        user2.setStatus(1);
        when(sysUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(user1, user2));

        Result<Long> result = controller.createAndSendNotification(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(notificationService, times(2)).createNotification(any(), eq("system"), eq("System notice"), eq("Content"), eq("broadcast"), eq("300"));
    }

    @Test
    void publishNotificationShouldBroadcast() {
        SysAdminNotification notification = buildNotification(100L, 0);
        notification.setType("update");
        notification.setTargetType("vip");
        when(sysAdminNotificationService.getById(100L)).thenReturn(notification);

        SysUser vipUser = new SysUser();
        vipUser.setId(10L);
        vipUser.setRole(1);
        vipUser.setStatus(1);
        when(sysUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(vipUser));

        Result<Void> result = controller.publishNotification(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysAdminNotificationService).updateById(notification);
        verify(notificationService).createNotification(eq(10L), eq("update"), eq("System notice"), eq("Content"), eq("broadcast"), eq("100"));
    }

    @Test
    void publishNotificationShouldErrorWhenAlreadyPublished() {
        SysAdminNotification notification = buildNotification(100L, 1);
        when(sysAdminNotificationService.getById(100L)).thenReturn(notification);

        Result<Void> result = controller.publishNotification(100L, authentication);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteNotificationShouldRemove() {
        Result<Void> result = controller.deleteNotification(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysAdminNotificationService).removeById(100L);
    }

    @Test
    void publishNotificationsBatchShouldPublishDraftsOnly() {
        SysAdminNotification draft = buildNotification(100L, 0);
        SysAdminNotification published = buildNotification(200L, 1);
        when(sysAdminNotificationService.listByIds(List.of(100L, 200L))).thenReturn(List.of(draft, published));
        when(sysUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Result<Void> result = controller.publishNotificationsBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(1, draft.getStatus());
        verify(sysAdminNotificationService).updateById(draft);
        verify(sysAdminNotificationService, never()).updateById(published);
    }

    @Test
    void publishNotificationsBatchShouldRejectEmptyIds() {
        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> controller.publishNotificationsBatch(List.of(), authentication)
        );
    }

    @Test
    void deleteNotificationsBatchShouldRemoveIds() {
        Result<Void> result = controller.deleteNotificationsBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysAdminNotificationService).removeByIds(List.of(100L, 200L));
    }

    private AdminNotificationCreateRequest buildRequest(int status) {
        AdminNotificationCreateRequest request = new AdminNotificationCreateRequest();
        request.setTitle("System notice");
        request.setContent("Content");
        request.setType("system");
        request.setTargetType("all");
        request.setStatus(status);
        return request;
    }

    private SysAdminNotification buildNotification(Long id, int status) {
        SysAdminNotification notification = new SysAdminNotification();
        notification.setId(id);
        notification.setTitle("System notice");
        notification.setContent("Content");
        notification.setType("system");
        notification.setTargetType("all");
        notification.setStatus(status);
        notification.setPublishedAt(LocalDateTime.now());
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }
}
