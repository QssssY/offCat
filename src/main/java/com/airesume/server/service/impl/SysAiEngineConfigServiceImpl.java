package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.mapper.SysAiEngineConfigMapper;
import com.airesume.server.service.SysAiEngineConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 管理端 AI 引擎配置服务实现。
 */
@Service
public class SysAiEngineConfigServiceImpl extends ServiceImpl<SysAiEngineConfigMapper, SysAiEngineConfig>
        implements SysAiEngineConfigService {

    @Override
    public List<SysAiEngineConfig> listAllOrdered() {
        LambdaQueryWrapper<SysAiEngineConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysAiEngineConfig::getBusinessType)
                .orderByDesc(SysAiEngineConfig::getIsActive)
                .orderByAsc(SysAiEngineConfig::getSort)
                .orderByAsc(SysAiEngineConfig::getId);
        return list(wrapper);
    }

    @Override
    public SysAiEngineConfig getByEngineCode(String engineCode) {
        if (engineCode == null || engineCode.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<SysAiEngineConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAiEngineConfig::getEngineCode, engineCode.trim())
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    @Override
    public boolean existsByEngineCode(String engineCode, Long excludeId) {
        if (engineCode == null || engineCode.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<SysAiEngineConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAiEngineConfig::getEngineCode, engineCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysAiEngineConfig::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    @Override
    public void validateBusinessType(String businessType) {
        String normalized = normalizeBusinessType(businessType);
        if (normalized == null || !AiEngineConstants.SUPPORTED_BUSINESS_TYPES.contains(normalized)) {
            throw new BusinessException("businessType 只支持 interview 或 resume");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(SysAiEngineConfig config) {
        String businessType = normalizeBusinessType(config.getBusinessType());
        validateBusinessType(businessType);
        config.setBusinessType(businessType);
        validateActiveFlag(config.getIsActive());

        if (AiEngineConstants.ACTIVE == config.getIsActive()) {
            deactivateOtherConfigs(config.getBusinessType(), null);
        }
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysAiEngineConfig config) {
        String businessType = normalizeBusinessType(config.getBusinessType());
        validateBusinessType(businessType);
        config.setBusinessType(businessType);
        validateActiveFlag(config.getIsActive());

        if (AiEngineConstants.ACTIVE == config.getIsActive()) {
            deactivateOtherConfigs(config.getBusinessType(), config.getId());
        }
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchActive(Long id, Integer isActive) {
        validateActiveFlag(isActive);

        SysAiEngineConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("AI 引擎配置不存在");
        }

        if (AiEngineConstants.ACTIVE == isActive) {
            deactivateOtherConfigs(config.getBusinessType(), config.getId());
        }

        config.setIsActive(isActive);
        updateById(config);
    }

    @Override
    public SysAiEngineConfig getActiveByBusinessType(String businessType) {
        String normalizedBusinessType = normalizeBusinessType(businessType);
        validateBusinessType(normalizedBusinessType);

        LambdaQueryWrapper<SysAiEngineConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAiEngineConfig::getBusinessType, normalizedBusinessType)
                .eq(SysAiEngineConfig::getIsActive, AiEngineConstants.ACTIVE)
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    /**
     * 将同一业务类型下的其他启用配置全部置为禁用。
     *
     * 这是后端保证“同业务仅一条启用配置”的核心规则。
     */
    private void deactivateOtherConfigs(String businessType, Long excludeId) {
        LambdaUpdateWrapper<SysAiEngineConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysAiEngineConfig::getBusinessType, businessType)
                .eq(SysAiEngineConfig::getIsActive, AiEngineConstants.ACTIVE)
                .ne(excludeId != null, SysAiEngineConfig::getId, excludeId)
                .set(SysAiEngineConfig::getIsActive, AiEngineConstants.INACTIVE);
        update(wrapper);
    }

    /**
     * 限制启用状态仅允许 0 / 1，保证单启用规则可预测。
     */
    private void validateActiveFlag(Integer isActive) {
        if (isActive == null || (AiEngineConstants.ACTIVE != isActive && AiEngineConstants.INACTIVE != isActive)) {
            throw new BusinessException("isActive 只支持 0 或 1");
        }
    }

    /**
     * 在校验和持久化前统一业务类型格式。
     */
    private String normalizeBusinessType(String businessType) {
        return businessType == null ? null : businessType.trim().toLowerCase(Locale.ROOT);
    }
}
