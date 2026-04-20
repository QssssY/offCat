package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 AI 引擎配置列表响应参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEngineConfigResponse {

    /**
     * 主键 ID。
     */
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
     * 提供方类型。
     */
    private String providerType;

    /**
     * 业务类型键。
     */
    private String businessType;

    /**
     * 业务类型中文描述。
     */
    private String businessTypeDesc;

    /**
     * 具体模型名。
     */
    private String modelName;

    /**
     * 提供方基础地址。
     */
    private String baseUrl;

    /**
     * 脱敏后的 API Key。
     * 列表接口故意不暴露原始密钥。
     */
    private String apiKey;

    /**
     * 温度参数。
     */
    private BigDecimal temperature;

    /**
     * 最大 token 数。
     */
    private Integer maxTokens;

    /**
     * 超时时间（毫秒）。
     */
    private Integer timeoutMs;

    /**
     * 启用状态。
     */
    private Integer isActive;

    /**
     * 启用状态描述。
     */
    private String isActiveDesc;

    /**
     * 排序值。
     */
    private Integer sort;

    /**
     * 备注（可选）。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
