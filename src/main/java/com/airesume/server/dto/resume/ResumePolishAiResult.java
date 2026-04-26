package com.airesume.server.dto.resume;

import lombok.Data;

import java.util.List;

/**
 * AI 返回的结构化润色结果。
 */
@Data
public class ResumePolishAiResult {

    /**
     * 润色后的简历文本。
     */
    private String polishedResumeText;

    /**
     * 修改说明列表。
     */
    private List<String> modificationNotes;
}
