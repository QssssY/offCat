package com.airesume.server.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 语音面试云端 TTS 合成请求。
 */
@Data
public class TtsSpeechRequest {

    /**
     * 待合成文本。前端按句提交，后端仍设置上限，防止异常长文本推高用户 TTS 成本。
     */
    @NotBlank(message = "TTS 文本不能为空")
    @Size(max = 600, message = "单次 TTS 文本不能超过 600 个字符")
    private String text;
}
