package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.feedback.AdminFeedbackResponse;
import com.airesume.server.dto.feedback.AdminFeedbackStatusUpdateRequest;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserFeedback;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserFeedbackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackControllerTest {

    @Mock private UserFeedbackService userFeedbackService;
    @Mock private SysUserService sysUserService;
    @Mock private Authentication authentication;

    private AdminFeedbackController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminFeedbackController(userFeedbackService, sysUserService);
        lenient().when(authentication.getPrincipal()).thenReturn(9L);
    }

    @Test
    void shouldReturnPagedFeedbackList() {
        UserFeedback feedback = buildFeedback();
        Page<UserFeedback> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(feedback));
        when(userFeedbackService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(sysUserService.getById(10L)).thenReturn(buildUser(10L, "tester"));

        Result<Map<String, Object>> result = controller.getFeedbackList(1, 20, "bug", 0, 10L, authentication);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<AdminFeedbackResponse> records = (List<AdminFeedbackResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("问题反馈", records.get(0).getTypeDesc());
        assertEquals("待处理", records.get(0).getStatusDesc());
        assertEquals(1, result.getData().get("total"));
    }

    @Test
    void shouldUpdateStatusAndHandlerFields() {
        UserFeedback feedback = buildFeedback();
        when(userFeedbackService.getById(100L)).thenReturn(feedback);
        AdminFeedbackStatusUpdateRequest request = new AdminFeedbackStatusUpdateRequest();
        request.setStatus(2);
        request.setAdminRemark(" 已处理 ");

        Result<Void> result = controller.updateFeedbackStatus(100L, request, authentication);

        ArgumentCaptor<UserFeedback> captor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(userFeedbackService).updateById(captor.capture());
        UserFeedback updated = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals(2, updated.getStatus());
        assertEquals("已处理", updated.getAdminRemark());
        assertEquals(9L, updated.getHandledBy());
        assertNotNull(updated.getHandledAt());
    }

    @Test
    void shouldReturnErrorWhenFeedbackNotFound() {
        when(userFeedbackService.getById(404L)).thenReturn(null);
        AdminFeedbackStatusUpdateRequest request = new AdminFeedbackStatusUpdateRequest();
        request.setStatus(1);

        Result<Void> result = controller.updateFeedbackStatus(404L, request, authentication);

        assertEquals(500, result.getCode());
    }

    @Test
    void shouldRejectEmptyBatchDeleteIds() {
        assertThrows(BusinessException.class, () -> controller.deleteFeedbackBatch(List.of(), authentication));
    }

    @Test
    void shouldDeleteBatchIds() {
        Result<Void> result = controller.deleteFeedbackBatch(List.of(100L, 200L), authentication);

        assertEquals(200, result.getCode());
        verify(userFeedbackService).removeByIds(List.of(100L, 200L));
    }

    private UserFeedback buildFeedback() {
        UserFeedback feedback = new UserFeedback();
        feedback.setId(100L);
        feedback.setUserId(10L);
        feedback.setType("bug");
        feedback.setTitle("无法提交简历");
        feedback.setContent("这里是一段用户反馈内容");
        feedback.setContact("user@example.com");
        feedback.setStatus(0);
        feedback.setCreateTime(LocalDateTime.now());
        return feedback;
    }

    private SysUser buildUser(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
