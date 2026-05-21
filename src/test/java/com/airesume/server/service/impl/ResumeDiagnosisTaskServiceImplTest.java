package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeDiagnosisTaskServiceImplTest {

    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    @Mock private ResumePolishRecordMapper resumePolishRecordMapper;

    private ResumeDiagnosisTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ResumeDiagnosisTask.class);
        service = new ResumeDiagnosisTaskServiceImpl(
                null, null, null, null, null, null, null,
                resumeJobMatchRecordMapper, resumePolishRecordMapper);
        ReflectionTestUtils.setField(service, "baseMapper", resumeDiagnosisTaskMapper);
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
    void getTaskByIdIsAnnotatedWithCacheable() throws NoSuchMethodException {
        java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                .getMethod("getTaskById", Long.class, Long.class);
        org.springframework.cache.annotation.Cacheable annotation =
                method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
        assertNotNull(annotation);
        assertEquals("resume:task", annotation.value()[0]);
    }

    @Test
    void updateStatusMethodsAreAnnotatedWithCacheEvict() throws NoSuchMethodException {
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
            assertNotNull(annotation, methods[i] + " should have @CacheEvict");
            assertEquals("resume:task", annotation.value()[0]);
            assertTrue(annotation.allEntries(), methods[i] + " should have allEntries = true");
        }
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
        ResumeDiagnosisTask orphan = new ResumeDiagnosisTask();
        orphan.setId(100L);
        orphan.setUserId(123L);
        orphan.setStatus(ResumeDiagnosisConstants.STATUS_PROCESSING);
        orphan.setStage(ResumeDiagnosisConstants.STAGE_AI_ANALYZING);
        orphan.setUpdateTime(LocalDateTime.now().minusMinutes(30));
        when(resumeDiagnosisTaskMapper.selectList(any())).thenReturn(List.of(orphan));

        int recoveredCount = service.recoverOrphanedTasks(10);

        ArgumentCaptor<ResumeDiagnosisTask> taskCaptor = ArgumentCaptor.forClass(ResumeDiagnosisTask.class);
        verify(resumeDiagnosisTaskMapper).updateById(taskCaptor.capture());
        assertEquals(1, recoveredCount);
        assertEquals(ResumeDiagnosisConstants.STATUS_FAILED, taskCaptor.getValue().getStatus());
        assertNull(taskCaptor.getValue().getStage());
        assertNotNull(taskCaptor.getValue().getFailedAt());
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
        doReturn(task).when(spyService).getById(100L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> spyService.retryFailedTask(100L, userId));

        assertEquals(ResultCode.RESUME_TASK_RETRY_EXPIRED.getCode(), exception.getCode());
        verify(spyService, never()).createTask(anyLong(), anyString());
    }
}
