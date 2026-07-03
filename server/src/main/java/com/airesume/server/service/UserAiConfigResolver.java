package com.airesume.server.service;

import com.airesume.server.dto.ai.ResolvedAiConfig;

/**
 * 用户自定义 AI 配置解析器。
 */
public interface UserAiConfigResolver {

    /**
     * 按 userId + businessType 解析用户自定义配置。
     *
     * @param userId 当前用户 ID
     * @param businessType 业务类型：resume/interview
     * @param fallbackToPlatform true 时忽略用户配置，强制使用平台 AI
     * @return 命中的用户配置，未命中返回 null
     */
    ResolvedAiConfig resolve(Long userId, String businessType, boolean fallbackToPlatform);
}
