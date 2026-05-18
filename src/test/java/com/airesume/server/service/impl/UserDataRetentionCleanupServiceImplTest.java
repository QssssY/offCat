package com.airesume.server.service.impl;

import com.airesume.server.entity.UserSettings;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mapper.UserSettingsMapper;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataRetentionCleanupServiceImplTest {

    @Mock private UserSettingsMapper userSettingsMapper;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewMessageRepository interviewMessageRepository;
    @Mock private MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    @Mock private ResumePolishRecordMapper resumePolishRecordMapper;

    private UserDataRetentionCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDataRetentionCleanupServiceImpl(
                userSettingsMapper,
                interviewSessionRepository,
                interviewMessageRepository,
                mockInterviewJobTargetRecordMapper,
                resumeDiagnosisTaskMapper,
                resumeJobMatchRecordMapper,
                resumePolishRecordMapper);
    }

    @Test
    void shouldCleanupExpiredInterviewRecordsWithRelatedData() {
        UserSettings settings = new UserSettings();
        settings.setUserId(123L);
        settings.setInterviewRetentionDays(30);
        when(userSettingsMapper.selectInterviewRetentionEnabled()).thenReturn(List.of(settings));
        when(interviewSessionRepository.findExpiredSessionIds(eq(123L), eq(1), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of("s1", "s2"))
                .thenReturn(List.of());
        when(interviewSessionRepository.logicalDeleteBySessionIdIn(eq(List.of("s1", "s2")), any(LocalDateTime.class)))
                .thenReturn(2);

        int deleted = service.cleanupExpiredInterviewRecords();

        assertEquals(2, deleted);
        verify(interviewMessageRepository).logicalDeleteBySessionIdIn(eq(List.of("s1", "s2")), any(LocalDateTime.class));
        verify(mockInterviewJobTargetRecordMapper).logicalDeleteBySessionIds(List.of("s1", "s2"));
        verify(interviewSessionRepository).logicalDeleteBySessionIdIn(eq(List.of("s1", "s2")), any(LocalDateTime.class));
    }

    @Test
    void shouldSkipInterviewCleanupWhenRetentionDisabled() {
        int deleted = service.cleanupExpiredInterviewRecordsForUser(123L, 0);

        assertEquals(0, deleted);
        verify(interviewSessionRepository, never()).findExpiredSessionIds(anyLong(), anyInt(), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void shouldCleanupExpiredResumeRecordsWithRelatedDataAndSkipIllegalFilePath() {
        UserSettings settings = new UserSettings();
        settings.setUserId(123L);
        settings.setResumeRetentionDays(90);
        when(userSettingsMapper.selectResumeRetentionEnabled()).thenReturn(List.of(settings));
        when(resumeDiagnosisTaskMapper.selectExpiredTerminalTaskIds(eq(123L), eq(List.of(2, 3)), any(LocalDateTime.class), eq(200)))
                .thenReturn(List.of(10L, 11L))
                .thenReturn(List.of());
        when(resumeDiagnosisTaskMapper.selectActiveFileUrlsByTaskIds(List.of(10L, 11L)))
                .thenReturn(List.of("/uploads/resumes/missing.pdf", "../escape.pdf"));
        when(resumeDiagnosisTaskMapper.logicalDeleteByTaskIds(List.of(10L, 11L))).thenReturn(2);

        int deleted = service.cleanupExpiredResumeRecords();

        assertEquals(2, deleted);
        verify(resumeJobMatchRecordMapper).logicalDeleteByResumeTaskIds(List.of(10L, 11L));
        verify(resumePolishRecordMapper).logicalDeleteByResumeTaskIds(List.of(10L, 11L));
        verify(resumeDiagnosisTaskMapper).logicalDeleteByTaskIds(List.of(10L, 11L));
    }

    @Test
    void shouldUseCutoffTimeBasedOnRetentionDays() {
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(resumeDiagnosisTaskMapper.selectExpiredTerminalTaskIds(eq(123L), eq(List.of(2, 3)), cutoffCaptor.capture(), eq(200)))
                .thenReturn(List.of());

        int deleted = service.cleanupExpiredResumeRecordsForUser(123L, 30);

        assertEquals(0, deleted);
        assertTrue(cutoffCaptor.getValue().isBefore(LocalDateTime.now().minusDays(29)));
        assertTrue(cutoffCaptor.getValue().isAfter(LocalDateTime.now().minusDays(31)));
    }
}
