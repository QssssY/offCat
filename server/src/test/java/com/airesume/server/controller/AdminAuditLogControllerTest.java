package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserRightsChangeLog;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserRightsChangeLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private UserRightsChangeLogService userRightsChangeLogService;
    @Mock private SysUserService sysUserService;
    @Mock private Authentication authentication;

    private AdminAuditLogController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAuditLogController(userRightsChangeLogService, sysUserService);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getAuditLogsShouldReturnPagedResults() {
        UserRightsChangeLog logEntry = new UserRightsChangeLog();
        logEntry.setId(1L);
        logEntry.setUserId(10L);
        logEntry.setOperatorUserId(1L);
        logEntry.setBeforeRole(0);
        logEntry.setAfterRole(1);
        logEntry.setBeforeMembershipPlanCode("NORMAL");
        logEntry.setAfterMembershipPlanCode("vip_month");
        logEntry.setRemark("升级会员");
        logEntry.setCreateTime(LocalDateTime.now());

        Page<UserRightsChangeLog> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(logEntry));

        when(userRightsChangeLogService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        SysUser targetUser = new SysUser();
        targetUser.setUsername("testUser");
        SysUser operator = new SysUser();
        operator.setUsername("admin");

        when(sysUserService.getById(10L)).thenReturn(targetUser);
        when(sysUserService.getById(1L)).thenReturn(operator);

        Result<Map<String, Object>> result = controller.getAuditLogs(
                null, null, null, null, null, null, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        Map<String, Object> data = result.getData();
        assertEquals(1, ((List<?>) data.get("records")).size());
        assertEquals(1, data.get("total"));
    }

    @Test
    void getAuditLogsShouldFilterByUserId() {
        Page<UserRightsChangeLog> pageResult = new Page<>(1, 20, 0);
        pageResult.setRecords(List.of());

        when(userRightsChangeLogService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getAuditLogs(
                10L, null, null, null, null, null, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, ((List<?>) result.getData().get("records")).size());
    }
}
