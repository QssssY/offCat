package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TTS 模型/音色发现请求，仅需要地址和密钥。
 */
@Data
public class UserTtsDiscoveryRequest {

    /** TTS 服务基础地址，如 https://api.openai.com/v1 */
    @NotBlank(message = "TTS 地址不能为空")
    @Size(max = 512, message = "TTS 地址不能超过 512 个字符")
    private String baseUrl;

    /** TTS 服务 API Key */
    @NotBlank(message = "TTS API Key 不能为空")
    @Size(max = 1024, message = "TTS API Key 不能超过 1024 个字符")
    private String apiKey;

    /** TTS 提供商标识：openai/mimo，为空按 OpenAI 兜底。 */
    @Size(max = 32, message = "TTS Provider 标识不能超过 32 个字符")
    private String provider;
}
