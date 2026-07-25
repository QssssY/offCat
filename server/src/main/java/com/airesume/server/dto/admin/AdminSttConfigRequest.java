package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端系统 STT（语音识别）配置保存 / 测试请求。
 */
@Data
public class AdminSttConfigRequest {

    /** 是否启用系统级云端语音识别兜底。 */
    private Boolean enabled;

    /** 语音识别服务基础地址（OpenAI 兼容）。 */
    @Size(max = 255, message = "STT 地址长度不能超过255个字符")
    private String baseUrl;

    /** 明文 API Key，仅在保存时加密存储；编辑态可留空或传脱敏值以复用旧密钥。 */
    @Size(max = 512, message = "API Key 长度不能超过512个字符")
    private String apiKey;

    /** 语音识别模型标识，如 FunAudioLLM/SenseVoiceSmall。 */
    @Size(max = 128, message = "STT 模型长度不能超过128个字符")
    private String model;

    /** 语音转文字端点路径，默认 /audio/transcriptions。 */
    @Size(max = 128, message = "端点路径长度不能超过128个字符")
    private String endpointPath;
}
