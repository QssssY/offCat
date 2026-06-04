package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户侧系统级 TTS 可用状态响应。
 */
@Data
@Builder
public class SystemTtsStatusResponse {

    /**
     * 当前是否存在已启用且完整可用的系统级 TTS 配置；不返回任何系统配置明细，避免泄露服务地址和密钥信息。
     */
    private Boolean systemTtsAvailable;
}
