package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.SysConfig;
import com.airesume.server.mapper.SysConfigMapper;
import com.airesume.server.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统 key-value 配置服务实现。
 */
@Slf4j
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public int getCustomAiDailyLimit() {
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, UserAiConstants.CUSTOM_AI_DAILY_LIMIT_KEY)
                .last("limit 1"), false);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return UserAiConstants.DEFAULT_DAILY_LIMIT;
        }
        try {
            int limit = Integer.parseInt(config.getConfigValue().trim());
            return Math.max(1, limit);
        } catch (NumberFormatException e) {
            log.warn("自定义 AI 每日上限配置非法，回退默认值: {}", config.getConfigValue());
            return UserAiConstants.DEFAULT_DAILY_LIMIT;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomAiDailyLimit(int limit) {
        if (limit < 1 || limit > 10000) {
            throw new BusinessException("每日上限必须在 1 到 10000 之间");
        }
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, UserAiConstants.CUSTOM_AI_DAILY_LIMIT_KEY)
                .last("limit 1"), false);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(UserAiConstants.CUSTOM_AI_DAILY_LIMIT_KEY);
            config.setDescription("用户自定义API Key每日调用上限");
        }
        config.setConfigValue(String.valueOf(limit));
        saveOrUpdate(config);
    }
}
