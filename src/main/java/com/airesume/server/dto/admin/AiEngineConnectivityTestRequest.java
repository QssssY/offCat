package com.airesume.server.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 引擎连通测试请求。
 *
 * 新增配置时直接使用表单 API Key；编辑配置时如果 apiKey 为空，后端会使用已保存的真实密钥测试。
 */
@Data
public class AiEngineConnectivityTestRequest {

    /**
     * 编辑态配置 ID。新增态为空。
     */
    private Long id;

    /**
     * 提供方类型，例如 openai / doubao / mock。
     */
    @Size(max = 32, message = "providerType 不能超过32个字符")
    private String providerType;

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
     * 提供方 API Key。编辑态为空时沿用数据库真实密钥。
     */
    private String apiKey;

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
    @Max(value = 300000, message = "timeoutMs 不能大于 300000")
    private Integer timeoutMs;
}
