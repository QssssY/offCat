package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.feedback.UserFeedbackCreateRequest;
import com.airesume.server.entity.UserFeedback;
import com.airesume.server.service.UserFeedbackService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserFeedbackControllerTest {

    @Mock private UserFeedbackService userFeedbackService;
    @Mock private Authentication authentication;

    private UserFeedbackController controller;
    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new UserFeedbackController(userFeedbackService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        lenient().when(authentication.getPrincipal()).thenReturn(10L);
    }

    @Test
    void shouldCreateFeedbackWithPendingStatus() {
        UserFeedbackCreateRequest request = buildValidRequest();
        when(userFeedbackService.save(any(UserFeedback.class))).thenAnswer(invocation -> {
            UserFeedback feedback = invocation.getArgument(0);
            feedback.setId(100L);
            return true;
        });

        Result<Long> result = controller.createFeedback(request, authentication);

        ArgumentCaptor<UserFeedback> captor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(userFeedbackService).save(captor.capture());
        UserFeedback saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals(100L, result.getData());
        assertEquals(10L, saved.getUserId());
        assertEquals("bug", saved.getType());
        assertEquals("待处理问题", saved.getTitle());
        assertEquals(0, saved.getStatus());
    }

    @Test
    void shouldRejectInvalidFeedbackType() {
        UserFeedbackCreateRequest request = buildValidRequest();
        request.setType("invalid");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankTitleAndShortContent() {
        UserFeedbackCreateRequest request = buildValidRequest();
        request.setTitle(" ");
        request.setContent("太短");

        assertFalse(validator.validate(request).isEmpty());
    }

    private UserFeedbackCreateRequest buildValidRequest() {
        UserFeedbackCreateRequest request = new UserFeedbackCreateRequest();
        request.setType("bug");
        request.setTitle(" 待处理问题 ");
        request.setContent("这里是一段超过十个字符的问题描述");
        request.setContact("user@example.com");
        return request;
    }
}
