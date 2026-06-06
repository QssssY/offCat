package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.onboarding.OnboardingTasksResponse;
import com.airesume.server.entity.UserOnboardingState;
import com.airesume.server.entity.UserOnboardingTask;
import com.airesume.server.mapper.UserOnboardingStateMapper;
import com.airesume.server.mapper.UserOnboardingTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceImplTest {

    @Mock private UserOnboardingStateMapper stateMapper;
    @Mock private UserOnboardingTaskMapper taskMapper;

    private UserOnboardingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserOnboardingServiceImpl(taskMapper);
        ReflectionTestUtils.setField(service, "baseMapper", stateMapper);
    }

    // ===== getTasks =====

    @Test
    void shouldReturnAllTasksVisibleForNewUser() {
        // 无已完成任务
        when(taskMapper.selectList(any())).thenReturn(List.of());

        OnboardingTasksResponse response = service.getTasks(100L);

        assertTrue(response.getVisible());
        assertEquals(4, response.getTotalCount());
        assertEquals(0, response.getCompletedCount());
        assertFalse(response.getAllCompleted());
        assertEquals(4, response.getTasks().size());
        assertFalse(response.getTasks().get(0).getCompleted());
    }

    @Test
    void shouldReturnInvisibleWhenOldUserCompleted() {
        // 旧引导已完成
        UserOnboardingState completed = new UserOnboardingState();
        completed.setStatus("completed");
        completed.setGuideKey("v1_3_main_onboarding");
        doReturn(completed).when(stateMapper).selectOne(any(), anyBoolean());

        OnboardingTasksResponse response = service.getTasks(100L);

        assertFalse(response.getVisible());
        assertTrue(response.getAllCompleted());
        assertEquals(4, response.getCompletedCount());
    }

    @Test
    void shouldReturnInvisibleWhenOldUserSkipped() {
        UserOnboardingState skipped = new UserOnboardingState();
        skipped.setStatus("skipped");
        skipped.setGuideKey("v1_3_main_onboarding");
        doReturn(skipped).when(stateMapper).selectOne(any(), anyBoolean());

        OnboardingTasksResponse response = service.getTasks(100L);

        assertFalse(response.getVisible());
        assertTrue(response.getAllCompleted());
    }

    @Test
    void shouldTrackCompletedCount() {
        // 2 个任务已完成
        UserOnboardingTask t1 = new UserOnboardingTask();
        t1.setTaskKey("resume_uploaded");
        t1.setCompleted(1);
        UserOnboardingTask t2 = new UserOnboardingTask();
        t2.setTaskKey("report_viewed");
        t2.setCompleted(1);
        when(taskMapper.selectList(any())).thenReturn(List.of(t1, t2));

        OnboardingTasksResponse response = service.getTasks(100L);

        assertTrue(response.getVisible());
        assertEquals(2, response.getCompletedCount());
        assertEquals(4, response.getTotalCount());
        assertFalse(response.getAllCompleted());
    }

    @Test
    void shouldReturnAllCompletedWhenAllDone() {
        // 全部 4 个任务已完成
        UserOnboardingTask t1 = new UserOnboardingTask();
        t1.setTaskKey("resume_uploaded");
        t1.setCompleted(1);
        UserOnboardingTask t2 = new UserOnboardingTask();
        t2.setTaskKey("report_viewed");
        t2.setCompleted(1);
        UserOnboardingTask t3 = new UserOnboardingTask();
        t3.setTaskKey("jd_compared");
        t3.setCompleted(1);
        UserOnboardingTask t4 = new UserOnboardingTask();
        t4.setTaskKey("interview_completed");
        t4.setCompleted(1);
        when(taskMapper.selectList(any())).thenReturn(List.of(t1, t2, t3, t4));

        OnboardingTasksResponse response = service.getTasks(100L);

        assertTrue(response.getAllCompleted());
        assertFalse(response.getVisible());
        assertEquals(4, response.getCompletedCount());
    }

    // ===== completeTask =====

    @Test
    void shouldCompleteTaskIdempotently() {
        // 已存在且已完成
        UserOnboardingTask existing = new UserOnboardingTask();
        existing.setTaskKey("resume_uploaded");
        existing.setCompleted(1);
        when(taskMapper.selectOne(any())).thenReturn(existing);

        service.completeTask(100L, "resume_uploaded");

        verify(taskMapper, never()).insert(any(UserOnboardingTask.class));
        verify(taskMapper, never()).updateById(any(UserOnboardingTask.class));
    }

    @Test
    void shouldInsertNewTaskRecord() {
        // 不存在记录
        when(taskMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.insert(any(UserOnboardingTask.class))).thenReturn(1);

        service.completeTask(100L, "resume_uploaded");

        ArgumentCaptor<UserOnboardingTask> captor = ArgumentCaptor.forClass(UserOnboardingTask.class);
        verify(taskMapper).insert(captor.capture());
        UserOnboardingTask inserted = captor.getValue();
        assertEquals(100L, inserted.getUserId());
        assertEquals("resume_uploaded", inserted.getTaskKey());
        assertEquals(1, inserted.getCompleted());
        assertNotNull(inserted.getCompletedTime());
    }

    @Test
    void shouldRejectInvalidTaskKey() {
        assertThrows(BusinessException.class, () -> service.completeTask(100L, "invalid_key"));
        verify(taskMapper, never()).selectOne(any());
    }

    @Test
    void shouldHandleDuplicateKeyOnConcurrentInsert() {
        // 首次查询不存在
        when(taskMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(createTask("resume_uploaded", 0));
        when(taskMapper.insert(any(UserOnboardingTask.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(taskMapper.updateById(any(UserOnboardingTask.class))).thenReturn(1);

        service.completeTask(100L, "resume_uploaded");

        verify(taskMapper).insert(any(UserOnboardingTask.class));
        verify(taskMapper).updateById(any(UserOnboardingTask.class));
    }

    @Test
    void shouldUpdateExistingIncompleteTask() {
        UserOnboardingTask existing = createTask("report_viewed", 0);
        existing.setId(1L);
        when(taskMapper.selectOne(any())).thenReturn(existing);
        when(taskMapper.updateById(any(UserOnboardingTask.class))).thenReturn(1);

        service.completeTask(100L, "report_viewed");

        verify(taskMapper, never()).insert(any(UserOnboardingTask.class));
        ArgumentCaptor<UserOnboardingTask> captor = ArgumentCaptor.forClass(UserOnboardingTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getCompleted());
        assertNotNull(captor.getValue().getCompletedTime());
    }

    @Test
    void shouldCacheOnboardingStatusByUserAndGuideKey() throws Exception {
        Method method = UserOnboardingServiceImpl.class.getMethod("getStatus", Long.class, String.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertNotNull(cacheable);
        assertEquals("user:onboardingStatus", cacheable.value()[0]);
        assertEquals("#userId + '::' + #guideKey", cacheable.key());
        assertTrue(cacheable.sync());
    }

    @Test
    void shouldEvictOnboardingStatusAfterStateOrTaskUpdates() throws Exception {
        Method updateStatus = UserOnboardingServiceImpl.class.getMethod(
                "updateStatus", Long.class, com.airesume.server.dto.onboarding.OnboardingUpdateRequest.class);
        CacheEvict updateEvict = updateStatus.getAnnotation(CacheEvict.class);
        assertNotNull(updateEvict);
        assertEquals("user:onboardingStatus", updateEvict.value()[0]);
        assertEquals("#userId + '::' + #request.guideKey", updateEvict.key());

        Method completeTask = UserOnboardingServiceImpl.class.getMethod("completeTask", Long.class, String.class);
        CacheEvict completeEvict = completeTask.getAnnotation(CacheEvict.class);
        assertNotNull(completeEvict);
        assertEquals("user:onboardingStatus", completeEvict.value()[0]);
        assertEquals("#userId + '::' + 'v1_3_main_onboarding'", completeEvict.key());
    }

    @Test
    void shouldReturnSerializableOnboardingStatusForRedisCache() {
        assertTrue(Serializable.class.isAssignableFrom(
                com.airesume.server.dto.onboarding.OnboardingStatusResponse.class));
    }

    private UserOnboardingTask createTask(String key, int completed) {
        UserOnboardingTask task = new UserOnboardingTask();
        task.setTaskKey(key);
        task.setCompleted(completed);
        task.setUserId(100L);
        return task;
    }
}
