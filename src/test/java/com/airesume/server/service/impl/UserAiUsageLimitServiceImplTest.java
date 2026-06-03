package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.UserAiDailyUsage;
import com.airesume.server.mapper.UserAiDailyUsageMapper;
import com.airesume.server.mapper.UserAiUsageDetailMapper;
import com.airesume.server.service.SysConfigService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAiUsageLimitServiceImplTest {

    private UserAiDailyUsageMapper mapper;
    private UserAiUsageDetailMapper detailMapper;
    private SysConfigService sysConfigService;
    private UserAiUsageLimitServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserAiDailyUsage.class);
        mapper = mock(UserAiDailyUsageMapper.class);
        detailMapper = mock(UserAiUsageDetailMapper.class);
        sysConfigService = mock(SysConfigService.class);
        service = new UserAiUsageLimitServiceImpl(sysConfigService, detailMapper);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void shouldIncrementWhenBelowLimit() {
        when(sysConfigService.getCustomAiDailyLimit()).thenReturn(50);
        when(mapper.incrementIfBelowLimit(eq(1L), any(LocalDate.class), eq(50))).thenReturn(1);

        service.checkAndIncrement(1L);

        verify(mapper).insertEmptyUsageIfAbsent(anyLong(), eq(1L), any(LocalDate.class));
        verify(mapper).incrementIfBelowLimit(eq(1L), any(LocalDate.class), eq(50));
    }

    @Test
    void shouldIncrementTypedUsageDetailAfterTotalUsageCounted() {
        when(sysConfigService.getCustomAiDailyLimit()).thenReturn(50);
        when(mapper.incrementIfBelowLimit(eq(1L), any(LocalDate.class), eq(50))).thenReturn(1);
        when(detailMapper.incrementToday(eq(1L), any(LocalDate.class), eq(UserAiConstants.USAGE_TYPE_RESUME_POLISH)))
                .thenReturn(1);

        service.checkAndIncrement(1L, UserAiConstants.USAGE_TYPE_RESUME_POLISH);

        verify(detailMapper).insertEmptyDetailIfAbsent(
                anyLong(), eq(1L), any(LocalDate.class), eq(UserAiConstants.USAGE_TYPE_RESUME_POLISH));
        verify(detailMapper).incrementToday(
                eq(1L), any(LocalDate.class), eq(UserAiConstants.USAGE_TYPE_RESUME_POLISH));
    }

    @Test
    void shouldRejectWhenDailyLimitExceeded() {
        when(sysConfigService.getCustomAiDailyLimit()).thenReturn(1);
        when(mapper.incrementIfBelowLimit(eq(1L), any(LocalDate.class), eq(1))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.checkAndIncrement(1L));
    }

    @Test
    void shouldRollbackTodayUsage() {
        service.rollback(2L);

        verify(mapper).rollbackToday(eq(2L), any(LocalDate.class));
    }

    @Test
    void shouldRollbackTypedUsageDetail() {
        service.rollback(2L, UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE);

        verify(mapper).rollbackToday(eq(2L), any(LocalDate.class));
        verify(detailMapper).rollbackToday(
                eq(2L), any(LocalDate.class), eq(UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE));
    }

    @Test
    void shouldReturnUsageWithRemainingCount() {
        when(sysConfigService.getCustomAiDailyLimit()).thenReturn(50);
        UserAiDailyUsage usage = new UserAiDailyUsage();
        usage.setCallCount(12);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(usage);

        var response = service.getUsage(3L);

        assertEquals(12, response.getUsed());
        assertEquals(50, response.getLimit());
        assertEquals(38, response.getRemaining());
    }
}
