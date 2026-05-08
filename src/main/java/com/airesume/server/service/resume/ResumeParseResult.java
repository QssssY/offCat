package com.airesume.server.service.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一简历解析结果。
 * 供诊断主流程、结果页和后续能力复用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParseResult {

    public static final String MODE_TEXT = "TEXT";
    public static final String MODE_MULTIMODAL = "MULTIMODAL";
    public static final String MODE_OCR = "OCR";
    public static final String MODE_MIXED = "MIXED";

    /**
     * 解析得到的完整简历文本。
     */
    private String text;

    /**
     * 整体解析模式。
     */
    private String parseMode;

    /**
     * 返回给前端的解析提示。
     */
    private String parseMessage;
}
