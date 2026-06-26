package com.airesume.server.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 64, message = "engineCode 不能超过64个字符")
    private String engineCode;

    /**
     * 展示名称。
     */
    @Size(max = 100, message = "engineName 不能超过100个字符")
    private String engineName;

    /**
     * 提供方类型，例如 openai / doubao / mock。
     */
    @Size(max = 32, message = "providerType 不能超过32个字符")
    private String providerType;

    /**
     * 业务类型，当前支持 interview / resume。
     */
    private String businessType;

    /**
     * 具体模型名。
     */
    @Size(max = 128, message = "modelName 不能超过128个字符")
    private String modelName;

    /**
     * 提供方基础地址。
     */
    @Size(max = 255, message = "baseUrl 不能超过255个字符")
    @Pattern(regexp = "^https://.+", message = "baseUrl 只允许 https:// 地址")
    private String baseUrl;

    /**
     * 提供方 API Key。为 null 时沿用原值。
     */
    private String apiKey;

    /**
     * 是否支持多模态识别（图片型 PDF）。1-支持，0-不支持。
     */
    @Min(value = 0, message = "supportsMultimodal 只支持 0 或 1")
    @Max(value = 1, message = "supportsMultimodal 只支持 0 或 1")
    private Integer supportsMultimodal;

    /**
     * 思考模式。enabled=开启，disabled=关闭，none=不传 thinking 参数。
     */
    @Pattern(regexp = "^(enabled|disabled|none)$", message = "thinkingMode 只支持 enabled/disabled/none")
    private String thinkingMode;

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
