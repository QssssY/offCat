package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 AI 引擎模型列表获取请求。
 */
@Data
public class AdminAiEngineModelsRequest {

    /**
     * 编辑态配置 ID；未输入新 API Key 时用它读取已保存密钥。
     */
    private Long id;

    @Size(max = 32, message = "providerType 不能超过32个字符")
    private String providerType;

    @Size(max = 255, message = "baseUrl 不能超过255个字符")
    @Pattern(regexp = "^https://.+", message = "baseUrl 只允许 https:// 地址")
    private String baseUrl;

    /**
     * 新增态或更新密钥时提交的真实 API Key。
     */
    private String apiKey;

    @Min(value = 1000, message = "timeoutMs 不能小于 1000")
    @Max(value = 300000, message = "timeoutMs 不能大于 300000")
    private Integer timeoutMs;
}
