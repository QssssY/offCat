package com.airesume.server.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新 AI 引擎配置请求参数。
 *
 * 除 id 外其余字段均可选，便于管理端按需局部更新。
 */
@Data
public class AiEngineConfigUpdateRequest {

    /**
     * 待更新配置 ID。
     */
    @NotNull(message = "AI 引擎配置 ID 不能为空")
    private Long id;

    /**
     * 稳定引擎编码。
     */
    private String engineCode;

    /**
     * 展示名称。
     */
    private String engineName;

    /**
     * 提供方类型，例如 openai / doubao / mock。
     */
    private String providerType;

    /**
     * 业务类型，当前支持 interview / resume。
     */
    private String businessType;

    /**
     * 具体模型名。
     */
    private String modelName;

    /**
     * 提供方基础地址。
     */
    private String baseUrl;

    /**
     * 提供方 API Key。为 null 时沿用原值。
     */
    private String apiKey;

    /**
     * 模型生成温度参数。
     */
    @DecimalMin(value = "0.0", message = "temperature 不能小于 0")
    @DecimalMax(value = "2.0", message = "temperature 不能大于 2")
    private BigDecimal temperature;

    /**
     * 模型请求最大 token 数。
     */
    @Min(value = 1, message = "maxTokens 必须大于 0")
    private Integer maxTokens;

    /**
     * 超时时间（毫秒）。
     */
    @Min(value = 1000, message = "timeoutMs 不能小于 1000")
    private Integer timeoutMs;

    /**
     * 启用状态。同一 businessType 只能有一个启用配置。
     */
    @Min(value = 0, message = "isActive 只支持 0 或 1")
    @Max(value = 1, message = "isActive 只支持 0 或 1")
    private Integer isActive;

    /**
     * 管理端展示排序值。
     */
    @Min(value = 0, message = "sort 不能小于 0")
    private Integer sort;

    /**
     * 备注（可选）。空白字符串会被归一化为 null。
     */
    private String remark;
}
