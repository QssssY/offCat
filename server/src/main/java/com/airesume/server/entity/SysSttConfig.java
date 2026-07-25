package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 系统级 STT（语音识别）配置实体。
 *
 * 管理端只维护一套系统语音识别配置，供语音面试在浏览器 Web Speech 不可用时的云端兜底识别。
 * 与系统 TTS 配置完全独立，互不影响。
 */
@Data
@ToString(callSuper = true, exclude = "apiKey")
@EqualsAndHashCode(callSuper = true)
@TableName("sys_stt_config")
public class SysSttConfig extends BaseEntity {

    /** 单例键，固定为 1，用数据库唯一索引限制同一时间只存在一条有效配置。 */
    @TableField("singleton_key")
    private Integer singletonKey;

    /** STT 服务基础地址，OpenAI 兼容格式。 */
    @TableField("base_url")
    private String baseUrl;

    /** 加密后的 STT API Key。 */
    @TableField("api_key")
    private String apiKey;

    /** STT 模型标识，如 FunAudioLLM/SenseVoiceSmall。 */
    @TableField("model")
    private String model;

    /** STT 语音转文字端点路径。 */
    @TableField("endpoint_path")
    private String endpointPath;

    /** 是否启用系统 STT。 */
    @TableField("enabled")
    private Integer enabled;
}
