package com.airesume.server.dto.interview;

import lombok.Builder;
import lombok.Data;

/**
 * 语音面试云端语音识别（STT）结果响应。
 */
@Data
@Builder
public class SttTranscriptionResponse {

    /** 识别出的文本内容。识别为空时返回空串，由前端决定是否忽略。 */
    private String text;
}
