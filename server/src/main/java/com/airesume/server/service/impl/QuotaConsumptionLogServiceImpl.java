package com.airesume.server.service.impl;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.quota.ConsumptionLogResponse;
import com.airesume.server.entity.QuotaConsumptionLog;
import com.airesume.server.entity.SysConfig;
import com.airesume.server.mapper.QuotaConsumptionLogMapper;
import com.airesume.server.service.QuotaConsumptionLogService;
import com.airesume.server.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户额度消费记录服务实现
 * 提供消费记录写入、分页查询和定时清理能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaConsumptionLogServiceImpl extends ServiceImpl<QuotaConsumptionLogMapper, QuotaConsumptionLog>
        implements QuotaConsumptionLogService {

    private final SysConfigService sysConfigService;

    /** 消费记录保留天数配置键 */
    private static final String RETENTION_CONFIG_KEY = "consumption_log_retention_days";
    /** 默认保留天数 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    // ==================== 额度类型中文名映射 ====================
    private static final Map<String, String> QUOTA_TYPE_NAMES = new HashMap<>();
    static {
        QUOTA_TYPE_NAMES.put("INTERVIEW", "模拟面试");
        QUOTA_TYPE_NAMES.put("RESUME", "简历诊断");
        QUOTA_TYPE_NAMES.put("POLISH", "AI润色");
        QUOTA_TYPE_NAMES.put("JD_MATCH", "JD匹配");
        QUOTA_TYPE_NAMES.put("TEMPLATE", "模板库");
        QUOTA_TYPE_NAMES.put("OFFER", "Offer辅助");
    }

    // ==================== 扣减来源中文名映射 ====================
    private static final Map<String, String> SOURCE_NAMES = new HashMap<>();
    static {
        SOURCE_NAMES.put("FREE", "免费额度");
        SOURCE_NAMES.put("VIP_DAILY", "VIP每日额度");
        SOURCE_NAMES.put("VIP_CYCLE", "VIP周期额度");
    }

    // ==================== 业务类型中文名映射 ====================
    private static final Map<String, String> BUSINESS_TYPE_NAMES = new HashMap<>();
    static {
        BUSINESS_TYPE_NAMES.put("INTERVIEW_SESSION", "模拟面试会话");
        BUSINESS_TYPE_NAMES.put("RESUME_DIAGNOSIS", "简历诊断任务");
        BUSINESS_TYPE_NAMES.put("RESUME_POLISH", "AI润色操作");
        BUSINESS_TYPE_NAMES.put("JOB_MATCH", "JD匹配分析");
        BUSINESS_TYPE_NAMES.put("TEMPLATE_USE", "模板使用");
        BUSINESS_TYPE_NAMES.put("OFFER_ASSIST", "Offer辅助");
    }

    @Override
    public void logConsumption(Long userId, String quotaType, int changeAmount,
                               Integer balanceAfter, String source,
                               String billingSource, Long businessId,
                               String businessType, String description) {
        QuotaConsumptionLog logEntry = new QuotaConsumptionLog();
        logEntry.setUserId(userId);
        logEntry.setQuotaType(quotaType);
        logEntry.setChangeAmount(changeAmount);
        logEntry.setBalanceAfter(balanceAfter);
        logEntry.setSource(source);
        logEntry.setBillingSource(billingSource);
        logEntry.setBusinessId(businessId);
        logEntry.setBusinessType(businessType);
        logEntry.setDescription(description);
        save(logEntry);

        log.debug("记录额度消费: userId={}, quotaType={}, changeAmount={}, source={}, businessType={}",
                userId, quotaType, changeAmount, source, businessType);
    }

    @Override
    public PageResult<ConsumptionLogResponse> getUserConsumptionLog(Long userId, String quotaType,
                                                                     int pageNum, int pageSize) {
        return queryConsumptionLog(userId, quotaType, pageNum, pageSize);
    }

    @Override
    public PageResult<ConsumptionLogResponse> getAdminConsumptionLog(Long userId, String quotaType,
                                                                      int pageNum, int pageSize) {
        return queryConsumptionLog(userId, quotaType, pageNum, pageSize);
    }

    /**
     * 定时清理过期消费记录（每天凌晨3:00执行）
     * 从 sys_config 读取保留天数，默认90天
     */
    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredLogs() {
        int retentionDays = getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 逻辑删除过期记录
        long count = lambdaUpdate()
                .lt(QuotaConsumptionLog::getCreateTime, cutoff)
                .eq(QuotaConsumptionLog::getIsDeleted, 0)
                .set(QuotaConsumptionLog::getIsDeleted, 1)
                .update() ? 1 : 0;

        log.info("清理过期消费记录完成，清理时间线: {}，保留天数: {}", cutoff, retentionDays);
    }

    // ==================== 私有方法 ====================

    /**
     * 统一的分页查询逻辑，用户端和管理端共用
     */
    private PageResult<ConsumptionLogResponse> queryConsumptionLog(Long userId, String quotaType,
                                                                    int pageNum, int pageSize) {
        Page<QuotaConsumptionLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QuotaConsumptionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuotaConsumptionLog::getUserId, userId);
        wrapper.eq(QuotaConsumptionLog::getIsDeleted, 0);

        // 类型筛选
        if (quotaType != null && !quotaType.isBlank()) {
            wrapper.eq(QuotaConsumptionLog::getQuotaType, quotaType);
        }

        wrapper.orderByDesc(QuotaConsumptionLog::getCreateTime);

        Page<QuotaConsumptionLog> result = page(page, wrapper);

        List<ConsumptionLogResponse> responses = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 实体转响应 DTO，附带中文名映射
     */
    private ConsumptionLogResponse toResponse(QuotaConsumptionLog entity) {
        return ConsumptionLogResponse.builder()
                .id(entity.getId())
                .quotaType(entity.getQuotaType())
                .quotaTypeName(QUOTA_TYPE_NAMES.getOrDefault(entity.getQuotaType(), entity.getQuotaType()))
                .changeAmount(entity.getChangeAmount())
                .balanceAfter(entity.getBalanceAfter())
                .source(entity.getSource())
                .sourceName(SOURCE_NAMES.getOrDefault(entity.getSource(), entity.getSource()))
                .billingSource(entity.getBillingSource())
                .businessType(entity.getBusinessType())
                .businessTypeName(BUSINESS_TYPE_NAMES.getOrDefault(entity.getBusinessType(), entity.getBusinessType()))
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * 从 sys_config 表读取保留天数，默认90天
     */
    private int getRetentionDays() {
        try {
            SysConfig config = sysConfigService.getOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, RETENTION_CONFIG_KEY)
                    .last("limit 1"), false);
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
                return Math.max(1, Integer.parseInt(config.getConfigValue().trim()));
            }
        } catch (Exception e) {
            log.warn("读取消费记录保留天数配置失败，使用默认值: {}", DEFAULT_RETENTION_DAYS, e);
        }
        return DEFAULT_RETENTION_DAYS;
    }
}
