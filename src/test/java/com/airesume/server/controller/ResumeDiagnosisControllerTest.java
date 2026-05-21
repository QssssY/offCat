package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.user.DataCleanupResponse;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeDiagnosisControllerTest {

    @Mock
    private ResumeDiagnosisTaskService resumeDiagnosisTaskService;

    @Mock
    private ResumeJobMatchService resumeJobMatchService;

    @Mock
    private ResumePolishService resumePolishService;

    @Mock
    private Authentication authentication;

    private ResumeDiagnosisController controller;

    @BeforeEach
    void setUp() {
        controller = new ResumeDiagnosisController(
                resumeDiagnosisTaskService,
                resumeJobMatchService,
                resumePolishService);
    }

    @Test
    void shouldClearCurrentUserResumeHistory() {
        when(authentication.getPrincipal()).thenReturn(123L);
        when(resumeDiagnosisTaskService.clearHistory(123L)).thenReturn(4);

        Result<DataCleanupResponse> result = controller.clearHistory(authentication);

        assertEquals(200, result.getCode());
        assertEquals(4, result.getData().getDeletedCount());
        verify(resumeDiagnosisTaskService).clearHistory(123L);
    }

    @Test
    void uploadResumeShouldRejectEmptyFileWithResumeCode() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.uploadResume(file, authentication));

        assertEquals(ResultCode.RESUME_FILE_EMPTY.getCode(), exception.getCode());
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getMessage(), exception.getMessage());
    }

    @Test
    void uploadResumeShouldRejectDocxBeforeCreatingTask() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx".getBytes());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.uploadResume(file, authentication));

        assertEquals(ResultCode.RESUME_FORMAT_UNSUPPORTED.getCode(), exception.getCode());
        assertEquals(ResultCode.RESUME_FORMAT_UNSUPPORTED.getMessage(), exception.getMessage());
    }
}
