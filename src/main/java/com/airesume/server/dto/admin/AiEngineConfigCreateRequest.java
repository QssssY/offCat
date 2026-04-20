package com.airesume.server.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增 AI 引擎配置请求参数。
 */
@Data
public class AiEngineConfigCreateRequest {

    /**
     * 管理端使用的稳定引擎编码。
     */
    @NotBlank(message = "引擎编码不能为空")
    private String engineCode;

    /**
     * 管理端展示名称。
     */
    @NotBlank(message = "引擎名称不能为空")
    private String engineName;

    /**
     * 提供方类型，例如 openai / doubao / mock。
     */
    @NotBlank(message = "提供商类型不能为空")
    private String providerType;

    /**
     * 业务类型，当前支持 interview / resume。
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 传给 AI 提供方的具体模型名。
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * 提供方基础地址。
     */
    @NotBlank(message = "基础地址不能为空")
    private String baseUrl;

    /**
     * 提供方 API Key。
     */
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    /**
     * 模型生成温度参数。
     */
    @NotNull(message = "temperature 不能为空")
    @DecimalMin(value = "0.0", message = "temperature 不能小于 0")
    @DecimalMax(value = "2.0", message = "temperature 不能大于 2")
    private BigDecimal temperature;

    /**
     * 模型请求最大 token 数。
     */
    @NotNull(message = "maxTokens 不能为空")
    @Min(value = 1, message = "maxTokens 必须大于 0")
    private Integer maxTokens;

    /**
     * 超时时间（毫秒）。
     */
    @NotNull(message = "timeoutMs 不能为空")
    @Min(value = 1000, message = "timeoutMs 不能小于 1000")
    private Integer timeoutMs;

    /**
     * 启用状态。同一 businessType 只能有一个启用配置。
     */
    @NotNull(message = "isActive 不能为空")
    @Min(value = 0, message = "isActive 只支持 0 或 1")
    @Max(value = 1, message = "isActive 只支持 0 或 1")
    private Integer isActive;

    /**
     * 管理端展示排序值。
     */
    @NotNull(message = "sort 不能为空")
    @Min(value = 0, message = "sort 不能小于 0")
    private Integer sort;

    /**
     * 备注（可选）。
     */
    private String remark;
}
