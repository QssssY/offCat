package com.airesume.server.service.impl;

import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStatsServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private PublicStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicStatsServiceImpl(sysUserMapper, resumeDiagnosisTaskMapper, interviewSessionMapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldUseRefreshLockAndPopulatePrimaryAndStaleCachesOnMiss() {
        when(valueOperations.get("public:stats")).thenReturn(null);
        when(valueOperations.setIfAbsent("public:stats:lock", "1", 5, TimeUnit.SECONDS)).thenReturn(true);
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
        when(resumeDiagnosisTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(20L);
        when(interviewSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(30L);

        Map<String, Long> stats = service.getPublicStats();

        assertEquals(Map.of("userCount", 10L, "diagnosisCount", 20L, "interviewCount", 30L), stats);
        verify(valueOperations).set("public:stats", stats, 5, TimeUnit.MINUTES);
        verify(valueOperations).set("public:stats:stale", stats, 10, TimeUnit.MINUTES);
        verify(redisTemplate).delete("public:stats:lock");
    }

    @Test
    void shouldReturnStaleStatsWithoutDatabaseQueryWhenRefreshLockIsBusy() {
        Map<String, Long> staleStats = Map.of("userCount", 1L, "diagnosisCount", 2L, "interviewCount", 3L);
        when(valueOperations.get("public:stats")).thenReturn(null);
        when(valueOperations.setIfAbsent("public:stats:lock", "1", 5, TimeUnit.SECONDS)).thenReturn(false);
        when(valueOperations.get("public:stats:stale")).thenReturn(staleStats);

        Map<String, Long> stats = service.getPublicStats();

        assertEquals(staleStats, stats);
        verify(sysUserMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(resumeDiagnosisTaskMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(interviewSessionMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }
}