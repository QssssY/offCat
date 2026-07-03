package com.airesume.server.service.impl;

import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class MockInterviewJobTargetServiceImplTest {

    @Test
    void shouldCacheEmptySessionContext() throws Exception {
        Method method = MockInterviewJobTargetServiceImpl.class.getMethod("getSessionContext", Long.class, String.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        // 空结果会返回可序列化的非 null 上下文，由 Spring Cache 负缓存，避免轮询详情反复查库。
        assertEquals("", cacheable.unless());
    }

    @Test
    void shouldReturnEmptyContextWhenSessionRecordMissing() {
        MockInterviewJobTargetServiceImpl service = spy(new MockInterviewJobTargetServiceImpl(
                mock(ResumeDiagnosisTaskMapper.class),
                mock(ResumeJobMatchService.class),
                mock(ResumeContentExtractor.class),
                new ObjectMapper()
        ));
        doReturn(null).when(service).getOne(any(Wrapper.class), eq(false));

        InterviewJobTargetContext context = service.getSessionContext(123L, "session-normal");

        assertNotNull(context);
        assertFalse(Boolean.TRUE.equals(context.getJobTargeted()));
        assertEquals("none", context.getSourceType());
    }
}
