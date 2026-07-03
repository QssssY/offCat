package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.onboarding.OnboardingTaskCompleteRequest;
import com.airesume.server.dto.onboarding.OnboardingTasksResponse;
import com.airesume.server.service.UserOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOnboardingControllerTest {

    @Mock private UserOnboardingService userOnboardingService;
    @Mock private Authentication authentication;

    private UserOnboardingController controller;

    @BeforeEach
    void setUp() {
        controller = new UserOnboardingController(userOnboardingService);
        when(authentication.getPrincipal()).thenReturn(123L);
    }

    @Test
    void shouldGetTasksForAuthenticatedUser() {
        OnboardingTasksResponse response = OnboardingTasksResponse.builder()
                .tasks(List.of())
                .completedCount(0)
                .totalCount(4)
                .allCompleted(false)
                .visible(true)
                .build();
        when(userOnboardingService.getTasks(123L)).thenReturn(response);

        Result<OnboardingTasksResponse> result = controller.getTasks(authentication);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(4, result.getData().getTotalCount());
        verify(userOnboardingService).getTasks(123L);
    }

    @Test
    void shouldCompleteTaskForAuthenticatedUser() {
        OnboardingTaskCompleteRequest request = new OnboardingTaskCompleteRequest();
        request.setTaskKey("resume_uploaded");

        Result<Void> result = controller.completeTask(request, authentication);

        assertEquals(200, result.getCode());
        verify(userOnboardingService).completeTask(123L, "resume_uploaded");
    }
}
