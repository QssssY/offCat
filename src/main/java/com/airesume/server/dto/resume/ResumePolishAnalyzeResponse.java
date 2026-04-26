package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 简历润色响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumePolishAnalyzeResponse {

    /**
     * 润色记录 ID。
     */
    private String polishRecordId;

    /**
     * 简历诊断任务 ID。
     */
    private String resumeTaskId;

    /**
     * 润色后的简历内容。
     */
    private String polishedResumeText;

    /**
     * 修改说明。
     */
    private List<String> modificationNotes;

    /**
     * 润色来源类型。
     */
    private String sourceType;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
