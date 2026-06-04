package com.airesume.server.service;

import com.airesume.server.dto.admin.AdminTtsConfigRequest;
import com.airesume.server.dto.admin.AdminTtsConfigResponse;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.entity.SysTtsConfig;

/**
 * 系统级 TTS 配置服务。
 */
public interface SysTtsConfigService {

    /**
     * 查询当前系统 TTS 配置，API Key 只返回脱敏值。
     */
    AdminTtsConfigResponse getCurrentConfig();

    /**
     * 保存系统 TTS 配置；编辑态 API Key 留空或传脱敏值时复用已保存密钥。
     */
    AdminTtsConfigResponse saveConfig(AdminTtsConfigRequest request);

    /**
     * 使用表单参数测试 TTS 连通性，不保存配置。
     */
    UserTtsConnectivityTestResponse testConnectivity(AdminTtsConfigRequest request);

    /**
     * 使用表单参数试听音色，不保存配置。
     */
    byte[] previewVoice(AdminTtsConfigRequest request);

    /**
     * 使用表单参数发现可用模型和音色，不保存配置。
     */
    UserTtsDiscoveryResponse discover(AdminTtsConfigRequest request);

    /**
     * 查询当前启用的系统 TTS 原始配置，密钥仍为加密态。
     */
    SysTtsConfig getEnabledConfigEntity();

    /**
     * 解析当前启用系统 TTS 配置，返回运行时可用的解密配置。
     */
    ResolvedTtsConfig resolveEnabledConfig();
}
