package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端系统级 TTS 配置请求。
 */
@Data
public class AdminTtsConfigRequest {

    /** 是否启用系统 TTS；启用后无用户自定义 TTS 的语音面试会使用该配置兜底。 */
    private Boolean enabled;

    /** TTS 提供商标识：openai/mimo 等，为空按 openai 兼容协议处理。 */
    @Size(max = 32, message = "TTS Provider 标识不能超过 32 个字符")
    private String ttsProvider;

    /** TTS 服务基础地址。 */
    @Size(max = 512, message = "TTS 地址不能超过 512 个字符")
    private String baseUrl;

    /** 明文 API Key；编辑时允许留空或传脱敏值以复用已保存密钥。 */
    @Size(max = 1024, message = "TTS API Key 不能超过 1024 个字符")
    private String apiKey;

    /** TTS 模型标识。 */
    @Size(max = 128, message = "TTS 模型不能超过 128 个字符")
    private String model;

    /** TTS 音色 ID。 */
    @Size(max = 128, message = "TTS 音色不能超过 128 个字符")
    private String voiceId;

    /** 合成端点路径，为空时默认 /audio/speech。 */
    @Size(max = 128, message = "TTS 端点路径不能超过 128 个字符")
    private String endpointPath;
}
