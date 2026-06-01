package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户自定义 OpenAI 兼容 AI 配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_ai_config")
public class UserAiConfig extends BaseEntity {

    /** 配置归属用户。 */
    @TableField("user_id")
    private Long userId;

    /** 配置类型：default/resume/interview。 */
    @TableField("config_type")
    private String configType;

    /** 用户自定义展示名称。 */
    @TableField("provider_name")
    private String providerName;

    /** OpenAI 兼容 API 基础地址。 */
    @TableField("base_url")
    private String baseUrl;

    /** AES/GCM 加密后的 API Key。 */
    @TableField("api_key")
    private String apiKey;

    /** 模型标识。 */
    @TableField("model")
    private String model;

    /** 是否启用当前配置。 */
    @TableField("is_enabled")
    private Integer isEnabled;

    /** 是否支持多模态，当前仅简历图片识别链路使用。 */
    @TableField("supports_multimodal")
    private Integer supportsMultimodal;

    /** 最后一次连通测试通过时间。 */
    @TableField("last_verified_at")
    private LocalDateTime lastVerifiedAt;

    /** 连通状态：pending/verified/failed。 */
    @TableField("verification_status")
    private String verificationStatus;

    /** TTS 预留字段，本期不暴露前端也不参与调用。 */
    @TableField("tts_base_url")
    private String ttsBaseUrl;

    @TableField("tts_api_key")
    private String ttsApiKey;

    @TableField("tts_model")
    private String ttsModel;

    @TableField("tts_voice_id")
    private String ttsVoiceId;
}
