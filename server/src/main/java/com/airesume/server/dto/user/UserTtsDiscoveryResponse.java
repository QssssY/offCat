package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TTS 模型/音色发现响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTtsDiscoveryResponse {

    /** 是否成功（模型列表获取成功即视为成功，音色可能回落到预设） */
    private Boolean success;

    /** 前端展示消息 */
    private String message;

    /** 发现的 TTS 模型列表 */
    private List<TtsModelOption> models;

    /** 发现的或预设的音色列表 */
    private List<TtsVoiceOption> voices;

    /** 音色是否由服务端实时发现（true），还是回落到了预设（false） */
    private Boolean voiceDiscoverySupported;

    /** 探测到的 TTS 合成端点路径（如 /audio/speech），前端随配置保存，合成时使用 */
    private String ttsEndpointPath;

    /** 失败原因，成功时为空 */
    private String errorMessage;
}
