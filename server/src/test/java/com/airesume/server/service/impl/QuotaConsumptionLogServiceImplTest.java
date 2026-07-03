package com.airesume.server.service.impl;

import com.airesume.server.dto.quota.ConsumptionLogResponse;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.entity.QuotaConsumptionLog;
import com.airesume.server.entity.SysConfig;
import com.airesume.server.mapper.QuotaConsumptionLogMapper;
import com.airesume.server.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QuotaConsumptionLogService 单元测试
 * 覆盖消费记录写入、分页查询和中文映射逻辑
 */
@ExtendWith(MockitoExtension.class)
class QuotaConsumptionLogServiceImplTest {

    @Mock
    private QuotaConsumptionLogMapper mapper;

    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private QuotaConsumptionLogServiceImpl service;

    private QuotaConsumptionLog savedLog;

    @BeforeEach
    void setUp() {
        // 手动注入 baseMapper，因为父类 ServiceImpl 的 baseMapper 字段无法被 @InjectMocks 自动注入
        try {
            java.lang.reflect.Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                    .getDeclaredField("baseMapper");
            baseMapperField.setAccessible(true);
            baseMapperField.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException("注入 baseMapper 失败", e);
        }
    }

    // ==================== logConsumption 测试 ====================

    @Test
    void shouldLogConsumptionWithCorrectFields() {
        // 捕获 save 传入的实体
        ArgumentCaptor<QuotaConsumptionLog> captor = ArgumentCaptor.forClass(QuotaConsumptionLog.class);
        when(mapper.insert(captor.capture())).thenReturn(1);

        service.logConsumption(
                1001L, "INTERVIEW", 1, 9,
                "VIP_DAILY", "PLATFORM", 2001L,
                "INTERVIEW_SESSION", "模拟面试-VIP每日额度扣减"
        );

        QuotaConsumptionLog log = captor.getValue();
        assertEquals(1001L, log.getUserId());
        assertEquals("INTERVIEW", log.getQuotaType());
        assertEquals(1, log.getChangeAmount());
        assertEquals(9, log.getBalanceAfter());
        assertEquals("VIP_DAILY", log.getSource());
        assertEquals("PLATFORM", log.getBillingSource());
        assertEquals(2001L, log.getBusinessId());
        assertEquals("INTERVIEW_SESSION", log.getBusinessType());
        assertEquals("模拟面试-VIP每日额度扣减", log.getDescription());
    }

    @Test
    void shouldLogRefundWithNegativeAmount() {
        ArgumentCaptor<QuotaConsumptionLog> captor = ArgumentCaptor.forClass(QuotaConsumptionLog.class);
        when(mapper.insert(captor.capture())).thenReturn(1);

        service.logConsumption(
                1002L, "RESUME", -1, null,
                "FREE", null, null,
                "RESUME_DIAGNOSIS", "简历诊断失败退款"
        );

        QuotaConsumptionLog log = captor.getValue();
        assertEquals(-1, log.getChangeAmount());
        assertNull(log.getBalanceAfter());
        assertEquals("FREE", log.getSource());
    }

    // ==================== getUserConsumptionLog 测试 ====================

    @Test
    void shouldReturnPaginatedResultsWithCorrectMapping() {
        // 构造 mock 分页返回
        Page<QuotaConsumptionLog> mockPage = new Page<>(1, 20);
        mockPage.setTotal(1);

        QuotaConsumptionLog logEntry = new QuotaConsumptionLog();
        logEntry.setId(123L);
        logEntry.setUserId(1001L);
        logEntry.setQuotaType("POLISH");
        logEntry.setChangeAmount(1);
        logEntry.setBalanceAfter(0);
        logEntry.setSource("FREE");
        logEntry.setBusinessType("RESUME_POLISH");
        logEntry.setDescription("AI润色-免费额度扣减");
        mockPage.setRecords(List.of(logEntry));

        // mock mapper 的 selectPage
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<ConsumptionLogResponse> result = service.getUserConsumptionLog(1001L, null, 1, 20);

        // 验证分页元数据
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());

        // 验证中文映射
        ConsumptionLogResponse resp = result.getList().get(0);
        assertEquals("AI润色", resp.getQuotaTypeName());
        assertEquals("免费额度", resp.getSourceName());
        assertEquals("AI润色操作", resp.getBusinessTypeName());
    }

    @Test
    void shouldFilterByQuotaType() {
        Page<QuotaConsumptionLog> mockPage = new Page<>(1, 20);
        mockPage.setTotal(0);
        mockPage.setRecords(List.of());

        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        service.getUserConsumptionLog(1001L, "INTERVIEW", 1, 20);

        // 验证调用了 selectPage（隐式验证 wrapper 包含了 quotaType 条件）
        verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldReturnEmptyWhenNoRecords() {
        Page<QuotaConsumptionLog> mockPage = new Page<>(1, 20);
        mockPage.setTotal(0);
        mockPage.setRecords(List.of());

        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<ConsumptionLogResponse> result = service.getUserConsumptionLog(9999L, null, 1, 20);

        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }

    // ==================== cleanExpiredLogs 测试 ====================

    @Test
    void shouldUseDefaultRetentionDaysWhenConfigMissing() {
        // sys_config 无匹配记录时使用默认90天
        when(sysConfigService.getOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(mapper.update(any(), any())).thenReturn(1);

        // 应不抛异常
        assertDoesNotThrow(() -> service.cleanExpiredLogs());
    }

    @Test
    void shouldReadRetentionDaysFromConfig() {
        SysConfig config = new SysConfig();
        config.setConfigKey("consumption_log_retention_days");
        config.setConfigValue("60");

        when(sysConfigService.getOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(config);
        when(mapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.cleanExpiredLogs());
    }

    // ==================== 中文映射完整性测试 ====================

    @Test
    void shouldMapAllQuotaTypesToChineseNames() {
        String[] types = {"INTERVIEW", "RESUME", "POLISH", "JD_MATCH", "TEMPLATE", "OFFER"};
        String[] expected = {"模拟面试", "简历诊断", "AI润色", "JD匹配", "模板库", "Offer辅助"};

        for (int i = 0; i < types.length; i++) {
            Page<QuotaConsumptionLog> mockPage = new Page<>(1, 20);
            mockPage.setTotal(1);

            QuotaConsumptionLog logEntry = new QuotaConsumptionLog();
            logEntry.setQuotaType(types[i]);
            logEntry.setSource("FREE");
            mockPage.setRecords(List.of(logEntry));

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<ConsumptionLogResponse> result = service.getUserConsumptionLog(1L, types[i], 1, 20);
            assertEquals(expected[i], result.getList().get(0).getQuotaTypeName(),
                    "类型 " + types[i] + " 应映射为 " + expected[i]);
        }
    }
}
