package com.airesume.server.service;

import com.airesume.server.dto.admin.AdminSttConfigRequest;
import com.airesume.server.dto.admin.AdminSttConfigResponse;
import com.airesume.server.dto.admin.AdminSttConnectivityTestResponse;
import com.airesume.server.dto.user.ResolvedSttConfig;
import com.airesume.server.dto.user.UserSttDiscoveryResponse;

/**
 * 系统级语音识别（STT）配置服务。
 * <p>
 * 管理端维护一套系统级 STT 配置，供语音面试在浏览器 Web Speech 不可用时做云端识别兜底。
 * 与面试对话 AI、语音播报 TTS 的配置完全独立，互不影响。
 */
public interface SysSttConfigService {

    /**
     * 查询当前系统 STT 配置，API Key 只返回脱敏值。
     */
    AdminSttConfigResponse getCurrentConfig();

    /**
     * 保存系统 STT 配置；编辑态 API Key 留空或传脱敏值时复用已保存密钥。
     */
    AdminSttConfigResponse saveConfig(AdminSttConfigRequest request);

    /**
     * 使用表单参数测试 STT 连通性，不保存配置。
     */
    AdminSttConnectivityTestResponse testConnectivity(AdminSttConfigRequest request);

    /**
     * 使用表单参数发现可用 STT 模型列表（调用 OpenAI 兼容 GET /models），不保存配置。
     */
    UserSttDiscoveryResponse discoverModels(AdminSttConfigRequest request);

    /**
     * 云端 STT 当前是否可用（已启用且配置完整）。
     */
    boolean isCloudSttAvailable();

    /**
     * 解析当前启用的系统 STT 配置，返回运行时可用的解密配置；不可用时返回 null。
     */
    ResolvedSttConfig resolveEnabledConfig();
}
