package com.airesume.server.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历文档编辑保存请求。
 */
@Data
public class ResumeDocumentUpdateRequest {

    /**
     * 编辑后的简历文档 JSON（包含 header、sections 等结构化数据）。
     */
    @NotBlank(message = "文档内容不能为空")
    @Size(max = 500000, message = "文档内容过长")
    private String documentJson;

    /**
     * 编辑后的简历纯文本（用于复制、搜索等场景）。
     */
    @Size(max = 65535, message = "纯文本内容过长")
    private String editedPlainText;
}
