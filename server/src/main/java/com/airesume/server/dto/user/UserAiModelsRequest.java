package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自定义 AI 模型列表获取请求。
 */
@Data
public class UserAiModelsRequest {

    @NotBlank(message = "API 地址不能为空")
    @Size(max = 512, message = "API 地址不能超过 512 个字符")
    private String baseUrl;

    @NotBlank(message = "API Key 不能为空")
    @Size(max = 1024, message = "API Key 不能超过 1024 个字符")
    private String apiKey;
}
