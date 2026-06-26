package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 模型选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsModelOption {

    /** 模型 ID，提交到 TTS 接口的 model 参数值 */
    private String id;

    /** 显示名称，通常与 id 相同 */
    private String name;
}
