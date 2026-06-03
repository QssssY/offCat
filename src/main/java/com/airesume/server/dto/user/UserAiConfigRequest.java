package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自定义 AI 配置保存请求。
 */
@Data
public class UserAiConfigRequest {

    /** 配置类型：default/resume/interview。 */
    @NotBlank(message = "配置类型不能为空")
    @Pattern(regexp = "default|resume|interview", message = "配置类型只支持 default/resume/interview")
    private String configType;

    /** 用户自定义名称。 */
    @Size(max = 64, message = "配置名称不能超过 64 个字符")
    private String providerName;

    /** OpenAI 兼容 API 基础地址。 */
    @NotBlank(message = "API 地址不能为空")
    @Size(max = 512, message = "API 地址不能超过 512 个字符")
    private String baseUrl;

    /** 真实 API Key。 */
    @NotBlank(message = "API Key 不能为空")
    @Size(max = 1024, message = "API Key 不能超过 1024 个字符")
    private String apiKey;

    /** 模型名称。 */
    @NotBlank(message = "模型不能为空")
    @Size(max = 128, message = "模型不能超过 128 个字符")
    private String model;

    /** 是否支持多模态。 */
    private Boolean supportsMultimodal;

    /** OpenAI 兼容 TTS 基础地址，留空表示不启用 TTS 配置。 */
    @Size(max = 512, message = "TTS 地址不能超过 512 个字符")
    private String ttsBaseUrl;

    /** TTS API Key。 */
    @Size(max = 1024, message = "TTS API Key 不能超过 1024 个字符")
    private String ttsApiKey;

    /** TTS 模型名称。 */
    @Size(max = 128, message = "TTS 模型不能超过 128 个字符")
    private String ttsModel;

    /** TTS 音色 ID。 */
    @Size(max = 128, message = "TTS 音色不能超过 128 个字符")
    private String ttsVoiceId;

    /** TTS 合成端点路径，由发现接口自动探测填入。 */
    @Size(max = 128, message = "TTS 端点路径不能超过 128 个字符")
    private String ttsEndpointPath;

    /** TTS 提供商标识：openai/mimo，为空按 OpenAI 兜底。 */
    @Size(max = 32, message = "TTS Provider 标识不能超过 32 个字符")
    private String ttsProvider;
}
