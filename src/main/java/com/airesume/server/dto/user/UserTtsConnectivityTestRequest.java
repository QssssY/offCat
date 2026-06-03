package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自定义 TTS 连通测试请求。
 */
@Data
public class UserTtsConnectivityTestRequest {

    /** OpenAI 兼容 TTS 基础地址。 */
    @NotBlank(message = "TTS 地址不能为空")
    @Size(max = 512, message = "TTS 地址不能超过 512 个字符")
    private String baseUrl;

    /** TTS API Key 明文，仅用于连通测试或保存前验证。 */
    @NotBlank(message = "TTS API Key 不能为空")
    @Size(max = 1024, message = "TTS API Key 不能超过 1024 个字符")
    private String apiKey;

    /** TTS 模型名称。 */
    @NotBlank(message = "TTS 模型不能为空")
    @Size(max = 128, message = "TTS 模型不能超过 128 个字符")
    private String model;

    /** TTS 音色 ID。 */
    @NotBlank(message = "TTS 音色不能为空")
    @Size(max = 128, message = "TTS 音色不能超过 128 个字符")
    private String voiceId;

    /** TTS 合成端点路径，为空时默认 /audio/speech。 */
    @Size(max = 128, message = "TTS 端点路径不能超过 128 个字符")
    private String endpointPath;

    /** TTS 提供商标识：openai/mimo，为空按 OpenAI 兜底。 */
    @Size(max = 32, message = "TTS Provider 标识不能超过 32 个字符")
    private String ttsProvider;

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }
}
