package com.airesume.server.service;

import com.airesume.server.dto.user.UserAiConfigRequest;
import com.airesume.server.dto.user.UserAiConfigResponse;
import com.airesume.server.dto.user.UserAiConnectivityTestRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestResponse;
import com.airesume.server.entity.UserAiConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 用户自定义 AI 配置管理服务。
 */
public interface UserAiConfigService extends IService<UserAiConfig> {

    /**
     * 查询当前用户全部配置，API Key 已脱敏。
     */
    List<UserAiConfigResponse> listUserConfigs(Long userId);

    /**
     * 创建或更新指定类型配置。
     */
    UserAiConfigResponse saveUserConfig(Long userId, UserAiConfigRequest request);

    /**
     * 删除指定类型配置。
     */
    void deleteUserConfig(Long userId, String configType);

    /**
     * 启用或禁用指定类型配置。
     */
    void toggleUserConfig(Long userId, String configType, boolean enabled);

    /**
     * 独立连通测试，不保存配置。
     */
    UserAiConnectivityTestResponse testConnectivity(UserAiConnectivityTestRequest request);

    /**
     * 查询启用的用户配置。
     */
    UserAiConfig findEnabledConfig(Long userId, String configType);
}
