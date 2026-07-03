package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 系统级 TTS 配置实体。
 *
 * 管理端只维护一套系统语音合成配置，供未配置自定义 TTS 的用户语音面试兜底使用。
 */
@Data
@ToString(callSuper = true, exclude = "apiKey")
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tts_config")
public class SysTtsConfig extends BaseEntity {

    /** 单例键，固定为 1，用数据库唯一索引限制同一时间只存在一条有效配置。 */
    @TableField("singleton_key")
    private Integer singletonKey;

    /** TTS 提供商标识：openai/mimo 等。 */
    @TableField("tts_provider")
    private String ttsProvider;

    /** TTS 服务基础地址。 */
    @TableField("base_url")
    private String baseUrl;

    /** 加密后的 TTS API Key。 */
    @TableField("api_key")
    private String apiKey;

    /** TTS 模型标识。 */
    @TableField("model")
    private String model;

    /** TTS 音色 ID。 */
    @TableField("voice_id")
    private String voiceId;

    /** TTS 合成端点路径。 */
    @TableField("endpoint_path")
    private String endpointPath;

    /** 是否启用系统 TTS。 */
    @TableField("enabled")
    private Integer enabled;
}
