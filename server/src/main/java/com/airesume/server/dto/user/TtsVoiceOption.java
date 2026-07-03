package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 音色选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsVoiceOption {

    /** 音色 ID，提交到 TTS 接口的 voice 参数值 */
    private String id;

    /** 显示名称，预设音色为首字母大写格式 */
    private String name;
}
