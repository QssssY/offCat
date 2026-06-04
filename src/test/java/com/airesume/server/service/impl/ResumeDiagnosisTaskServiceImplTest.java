package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mq.DirectProcessRouter;
import com.airesume.server.mq.ResumeDiagnosisProducer;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeDiagnosisTaskServiceImplTest {

    @Mock private UserQuotaService userQuotaService;
    @Mock private ResumeDiagnosisProducer resumeDiagnosisProducer;
    @Mock private DirectProcessRouter directProcessRouter;
    @Mock private NotificationService notificationService;
    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    @Mock private ResumePolishRecordMapper resumePolishRecordMapper;
    @Mock private CacheManager cacheManager;
    @Mock private Cache resumeTaskCache;

    private ResumeDiagnosisTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ResumeDiagnosisTask.class);
        service = new ResumeDiagnosisTaskServiceImpl(
                userQuotaService, resumeDiagnosisProducer, directProcessRouter, null, notificationService, null, null,
                resumeJobMatchRecordMapper, resumePolishRecordMapper);
        ReflectionTestUtils.setField(service, "baseMapper", resumeDiagnosisTaskMapper);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        lenient().when(cacheManager.getCache("resume:task")).thenReturn(resumeTaskCache);
    }

    @Test
    void sanitizeOriginalFilenameShouldStripPathTraversal() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        assertEquals("passwd", method.invoke(service, "../../etc/passwd"));
        assertEquals("test.pdf", method.invoke(service, "foo/bar/test.pdf"));
        assertEquals("evil.exe", method.invoke(service, "C:\\Windows\\evil.exe"));
        assertEquals("resume.pdf", method.invoke(service, "../resume.pdf"));
        assertEquals("file.pdf", method.invoke(service, "a/b/c/../file.pdf"));
    }

    @Test
    void sanitizeOriginalFilenameShouldRejectNull() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, (String) null));
        assertInstanceOf(BusinessException.class, ex.getCause());
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getCode(),
                ((BusinessException) ex.getCause()).getCode());
    }

    @Test
    void sanitizeOriginalFilenameShouldRejectBlank() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, ""));
        assertInstanceOf(BusinessException.class, ex.getCause());
    }

    @Test
    void buildStoredResumeFileNameShouldProduceUuidFormat() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("buildStoredResumeFileName");
        method.setAccessible(true);

        String fileName = (String) method.invoke(service);
        assertTrue(fileName.matches("\\d+_[a-f0-9]+\\.pdf"), "Expected format: timestamp_uuid.pdf, got: " + fileName);
    }

    @Test
    void shouldCreatePlatformTaskWithoutPrecheckingResumeQuota() {
        lenient().when(userQuotaService.checkResumeQuota(123L)).thenReturn(true);
        when(resumeDiagnosisTaskMapper.insert(any(ResumeDiagnosisTask.class))).thenAnswer(invocation -> {
            ResumeDiagnosisTask task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        });
        TransactionSynchronizationManager.initSynchronization();
        try {
            Long taskId = service.createTask(123L, "/uploads/resumes/resume.pdf", false);

            assertEquals(100L, taskId);
            verify(userQuotaService, never()).checkResumeQuota(123L);
            verify(userQuotaService).deductResumeQuota(123L);
            verify(notificationService, never()).createQuotaNotificationIfNeeded(123L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldNotifyWhenAtomicResumeQuotaDeductionReportsExhausted() {
        lenient().when(userQuotaService.checkResumeQuota(123L)).thenReturn(true);
        when(resumeDiagnosisTaskMapper.insert(any(ResumeDiagnosisTask.class))).thenAnswer(invocation -> {
            ResumeDiagnosisTask task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        });
        doThrow(new BusinessException(ResultCode.RESUME_QUOTA_EXHAUSTED))
                .when(userQuotaService).deductResumeQuota(123L);
        TransactionSynchronizationManager.initSynchronization();
        try {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createTask(123L, "/uploads/resumes/resume.pdf", false));

            assertEquals(ResultCode.RESUME_QUOTA_EXHAUSTED.getCode(), exception.getCode());
            verify(userQuotaService, never()).checkResumeQuota(123L);
            verify(notificationService).createQuotaNotificationIfNeeded(123L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRejectResumeUploadWhenDiskFreeSpaceBelowThreshold() {
        ResumeDiagnosisTaskServiceImpl lowSpaceService = new TestableResumeDiagnosisTaskServiceImpl(512L * 1024L * 1024L);
        ReflectionTestUtils.setField(lowSpaceService, "minFreeSpaceMb", 1024L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> lowSpaceService.ensureUploadDirectoryHasEnoughSpace(Path.of("uploads", "resumes")));

        assertEquals(ResultCode.RESUME_STORAGE_SPACE_LOW.getCode(), exception.getCode());
    }

    @Test
    void shouldAllowResumeUploadWhenDiskFreeSpaceMeetsThreshold() throws Exception {
        ResumeDiagnosisTaskServiceImpl enoughSpaceService = new TestableResumeDiagnosisTaskServiceImpl(2L * 1024L * 1024L * 1024L);
        ReflectionTestUtils.setField(enoughSpaceService, "minFreeSpaceMb", 1024L);

        assertDoesNotThrow(() -> enoughSpaceService.ensureUploadDirectoryHasEnoughSpace(Path.of("uploads", "resumes")));
    }

    @Test
    void getTaskByIdIsAnnotatedWithCacheable() throws NoSuchMethodException {
        java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                .getMethod("getTaskById", Long.class, Long.class);
        org.springframework.cache.annotation.Cacheable annotation =
                method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
        assertNotNull(annotation);
        assertEquals("resume:task", annotation.value()[0]);
    }

    @Test
    void updateStatusMethodsShouldNotClearWholeResumeTaskCache() throws NoSuchMethodException {
        String[] methods = {"updateStatusToProcessing", "updateStatusToCompleted",
                "updateStatusToFailed", "updateTaskResumeText", "updateTaskResumeParseResult"};
        Class<?>[][] paramTypes = {
                {Long.class},
                {Long.class, String.class},
                {Long.class, String.class},
                {Long.class, String.class},
                {Long.class, String.class, String.class, String.class}
        };
        for (int i = 0; i < methods.length; i++) {
            java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                    .getMethod(methods[i], paramTypes[i]);
            org.springframework.cache.annotation.CacheEvict annotation =
                    method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertTrue(annotation == null || !annotation.allEntries(),
                    methods[i] + " should not evict all resume task cache entries");
        }
    }

    @Test
    void shouldEvictOnlyUpdatedResumeTaskCacheEntry() {
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setId(100L);
        task.setUserId(123L);
        ResumeDiagnosisTaskServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).update(any(Wrapper.class));
        when(resumeDiagnosisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        spyService.updateStatusToCompleted(100L, "{}");

        verify(resumeTaskCache).evict("100::123");
        verify(resumeTaskCache, never()).clear();
    }

    @Test
    void getTaskStatusByIdShouldSelectOnlyLightweightColumns() {
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setId(100L);
        task.setUserId(123L);
        task.setStatus(ResumeDiagnosisConstants.STATUS_PROCESSING);
        task.setStage(ResumeDiagnosisConstants.STAGE_AI_ANALYZING);
        task.setErrorMsg(null);
        task.setCreateTime(LocalDateTime.now().minusMinutes(1));
        task.setUpdateTime(LocalDateTime.now());
        when(resumeDiagnosisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        var response = service.getTaskStatusById(100L, 123L);

        assertEquals("100", response.getTaskId());
        assertEquals(ResumeDiagnosisConstants.STATUS_PROCESSING, response.getStatus());
        ArgumentCaptor<Wrapper<ResumeDiagnosisTask>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resumeDiagnosisTaskMapper).selectOne(wrapperCaptor.capture());
        String sqlSelect = wrapperCaptor.getValue().getSqlSelect();
        assertNotNull(sqlSelect);
        assertTrue(sqlSelect.contains("status"));
        assertTrue(sqlSelect.contains("stage"));
        assertFalse(sqlSelect.contains("diagnosis_result"));
        assertFalse(sqlSelect.contains("resume_text"));
    }

    @Test
    void updateStatusToProcessingShouldReturnBoolean() throws NoSuchMethodException {
        java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                .getMethod("updateStatusToProcessing", Long.class);
        assertEquals(boolean.class, method.getReturnType(),
                "updateStatusToProcessing must return boolean for claim check");
    }

    @Test
    void shouldClearResumeHistoryAndRelatedRecords() {
        Long userId = 123L;
        when(resumeDiagnosisTaskMapper.selectActiveFileUrlsByUserId(userId)).thenReturn(List.of());
        when(resumeDiagnosisTaskMapper.logicalDeleteByUserId(userId)).thenReturn(3);

        int deletedCount = service.clearHistory(userId);

        assertEquals(3, deletedCount);
        verify(resumeJobMatchRecordMapper).logicalDeleteByUserId(userId);
        verify(resumePolishRecordMapper).logicalDeleteByUserId(userId);
        verify(resumeDiagnosisTaskMapper).logicalDeleteByUserId(userId);
    }

    @Test
    void shouldRejectUnsafeResumeFilePathWhenClearingHistory() {
        Long userId = 123L;
        when(resumeDiagnosisTaskMapper.selectActiveFileUrlsByUserId(userId))
                .thenReturn(List.of("/uploads/resumes/../../evil.pdf"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.clearHistory(userId));

        assertTrue(exception.getMessage().contains("路径不合法"));
        verify(resumeJobMatchRecordMapper).logicalDeleteByUserId(userId);
        verify(resumePolishRecordMapper).logicalDeleteByUserId(userId);
        verify(resumeDiagnosisTaskMapper).logicalDeleteByUserId(userId);
    }

    @Test
    void recoverOrphanedTasksShouldClearStageWhenMarkingFailed() {
        when(resumeDiagnosisTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ResumeDiagnosisTaskServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).update(any(Wrapper.class));

        int recoveredCount = spyService.recoverOrphanedTasks(10);

        ArgumentCaptor<Wrapper<ResumeDiagnosisTask>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(spyService).update(wrapperCaptor.capture());
        assertEquals(1, recoveredCount);
        String sqlSet = ((LambdaUpdateWrapper<ResumeDiagnosisTask>) wrapperCaptor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("status"));
        assertTrue(sqlSet.contains("stage"));
        assertTrue(sqlSet.contains("failed_at"));
    }

    @Test
    void updateStageShouldOnlyUpdateProcessingTask() {
        ResumeDiagnosisTaskServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).update(any(Wrapper.class));

        spyService.updateStage(100L, ResumeDiagnosisConstants.STAGE_AI_ANALYZING);

        ArgumentCaptor<Wrapper<ResumeDiagnosisTask>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(spyService).update(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("#{ew.paramNameValuePairs.MPGENVAL2}"));
    }

    @Test
    void updateStatusToFailedShouldSetFailedAt() {
        ResumeDiagnosisTaskServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).update(any(Wrapper.class));

        spyService.updateStatusToFailed(100L, "AI服务暂时不可用，请稍后重试");

        ArgumentCaptor<Wrapper<ResumeDiagnosisTask>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(spyService).update(wrapperCaptor.capture());
        String sqlSet = ((LambdaUpdateWrapper<ResumeDiagnosisTask>) wrapperCaptor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("failed_at"));
    }

    @Test
    void retryFailedTaskShouldUseFailedAtBeforeUpdateTime() {
        Long userId = 123L;
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setId(100L);
        task.setUserId(userId);
        task.setStatus(ResumeDiagnosisConstants.STATUS_FAILED);
        task.setFileUrl("/uploads/resumes/resume.pdf");
        task.setFailedAt(LocalDateTime.now().minusHours(25));
        task.setUpdateTime(LocalDateTime.now());
        ResumeDiagnosisTaskServiceImpl spyService = spy(service);
        when(resumeDiagnosisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> spyService.retryFailedTask(100L, userId));

        assertEquals(ResultCode.RESUME_TASK_RETRY_EXPIRED.getCode(), exception.getCode());
        verify(spyService, never()).createTask(anyLong(), anyString());
    }

    private static class TestableResumeDiagnosisTaskServiceImpl extends ResumeDiagnosisTaskServiceImpl {

        private final long usableSpace;

        TestableResumeDiagnosisTaskServiceImpl(long usableSpace) {
            super(null, null, null, null, null, null, null, null, null);
            this.usableSpace = usableSpace;
        }

        @Override
        protected long usableSpace(Path uploadDirPath) {
            return usableSpace;
        }
    }
}
