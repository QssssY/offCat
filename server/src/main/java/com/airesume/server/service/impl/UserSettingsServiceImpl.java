package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.UserSettingsRequest;
import com.airesume.server.dto.user.UserSettingsResponse;
import com.airesume.server.entity.UserSettings;
import com.airesume.server.mapper.UserSettingsMapper;
import com.airesume.server.service.UserSettingsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户设置服务实现。
 * 默认关闭自动清理，只有用户显式保存后才写入服务端并参与定时任务。
 */
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl extends ServiceImpl<UserSettingsMapper, UserSettings> implements UserSettingsService {

    private static final Set<Integer> ALLOWED_RETENTION_DAYS = Set.of(0, 30, 90, 180, 365);
    private static final int DEFAULT_RETENTION_DAYS = 0;

    @Override
    public UserSettingsResponse getSettings(Long userId) {
        return toResponse(getExistingOrDefault(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSettingsResponse saveSettings(Long userId, UserSettingsRequest request) {
        if (request == null) {
            throw new BusinessException("用户设置请求不能为空");
        }
        Integer interviewRetentionDays = normalizeRetentionDays(request.getInterviewRetentionDays());
        Integer resumeRetentionDays = normalizeRetentionDays(request.getResumeRetentionDays());

        UserSettings settings = getBaseMapper().selectActiveByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            settings.setCreateTime(now);
            settings.setIsDeleted(0);
        }

        settings.setInterviewRetentionDays(interviewRetentionDays);
        settings.setResumeRetentionDays(resumeRetentionDays);
        settings.setUpdateTime(now);

        if (settings.getId() == null) {
            save(settings);
        } else {
            updateById(settings);
        }
        return toResponse(settings);
    }

    private UserSettings getExistingOrDefault(Long userId) {
        UserSettings settings = getBaseMapper().selectActiveByUserId(userId);
        if (settings != null) {
            settings.setInterviewRetentionDays(normalizeRetentionDays(settings.getInterviewRetentionDays()));
            settings.setResumeRetentionDays(normalizeRetentionDays(settings.getResumeRetentionDays()));
            return settings;
        }

        UserSettings defaults = new UserSettings();
        defaults.setUserId(userId);
        defaults.setInterviewRetentionDays(DEFAULT_RETENTION_DAYS);
        defaults.setResumeRetentionDays(DEFAULT_RETENTION_DAYS);
        return defaults;
    }

    private Integer normalizeRetentionDays(Integer value) {
        int days = value == null ? DEFAULT_RETENTION_DAYS : value;
        if (value == null) {
            throw new BusinessException("保留天数不能为空，0 表示关闭自动清理");
        }
        if (!ALLOWED_RETENTION_DAYS.contains(days)) {
            throw new BusinessException("保留天数只能选择关闭、30天、90天、180天或365天");
        }
        return days;
    }

    private UserSettingsResponse toResponse(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getInterviewRetentionDays(),
                settings.getResumeRetentionDays()
        );
    }
}
