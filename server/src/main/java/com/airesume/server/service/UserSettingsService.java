package com.airesume.server.service;

import com.airesume.server.dto.user.UserSettingsRequest;
import com.airesume.server.dto.user.UserSettingsResponse;
import com.airesume.server.entity.UserSettings;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户设置服务。
 * 保存服务端真实生效的保留天数配置，与浏览器本地偏好区分。
 */
public interface UserSettingsService extends IService<UserSettings> {

    UserSettingsResponse getSettings(Long userId);

    UserSettingsResponse saveSettings(Long userId, UserSettingsRequest request);
}
