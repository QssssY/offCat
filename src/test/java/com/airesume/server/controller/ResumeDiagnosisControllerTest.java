package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.user.DataCleanupResponse;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeDiagnosisControllerTest {

    @Mock private ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    @Mock private ResumeJobMatchService resumeJobMatchService;
    @Mock private ResumePolishService resumePolishService;
    @Mock private Authentication authentication;

    private ResumeDiagnosisController controller;

    @BeforeEach
    void setUp() {
        controller = new ResumeDiagnosisController(
                resumeDiagnosisTaskService,
                resumeJobMatchService,
                resumePolishService);
        when(authentication.getPrincipal()).thenReturn(123L);
    }

    @Test
    void shouldClearCurrentUserResumeHistory() {
        when(resumeDiagnosisTaskService.clearHistory(123L)).thenReturn(4);

        Result<DataCleanupResponse> result = controller.clearHistory(authentication);

        assertEquals(200, result.getCode());
        assertEquals(4, result.getData().getDeletedCount());
        verify(resumeDiagnosisTaskService).clearHistory(123L);
    }
}
