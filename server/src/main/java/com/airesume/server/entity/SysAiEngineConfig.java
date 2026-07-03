package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * AI 引擎配置实体。
 *
 * 该表是管理端业务模型配置的唯一事实来源。
 */
@Data
@ToString(callSuper = true, exclude = "apiKey")
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_engine_config")
public class SysAiEngineConfig extends BaseEntity {

    /**
     * 稳定的引擎编码，用于管理端展示及后续业务引用。
     */
    @TableField("engine_code")
    private String engineCode;

    /**
     * 管理端列表展示名称。
     */
    @TableField("engine_name")
    private String engineName;

    /**
     * 模型提供方类型，例如 openai / doubao / mock。
     */
    @TableField("provider_type")
    private String providerType;

    /**
     * 该配置绑定的业务类型，当前支持 interview / resume。
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 实际调用模型时使用的模型名称。
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 提供方基础地址。
     */
    @TableField("base_url")
    private String baseUrl;

    /**
     * 提供方 API Key（敏感信息）。
     * 可以存库，但列表接口中禁止原样返回。
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 是否支持多模态识别（图片型 PDF）。1-支持，0-不支持。
     */
    @TableField("supports_multimodal")
    private Integer supportsMultimodal;

    /**
     * 思考模式：enabled / disabled / none。
     * none 表示不传 thinking 参数，使用模型默认行为。
     */
    @TableField("thinking_mode")
    private String thinkingMode;

    /**
     * 模型生成温度参数。
     */
    @TableField("temperature")
    private BigDecimal temperature;

    /**
     * 当前配置允许的最大 token 数。
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 超时时间（毫秒）。
     */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /**
     * 启用状态。
     * 后端必须保证同一业务类型同时仅一个配置处于启用状态。
     */
    @TableField("is_active")
    private Integer isActive;

    /**
     * 管理端展示排序值。
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 管理端备注（可选）。
     */
    @TableField("remark")
    private String remark;
}
