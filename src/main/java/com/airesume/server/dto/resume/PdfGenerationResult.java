package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * PDF 生成结果 DTO
 *
 * 用途：后端生成 PDF 后不再直接返回文件流，而是返回此 JSON 对象给前端，
 * 前端拿到 fileId 后可弹窗询问用户是否下载，确认后再请求下载接口。
 */
@Data
@AllArgsConstructor
public class PdfGenerationResult {

    /** 文件唯一标识（UUID，不含扩展名），前端用此 ID 调用下载接口 */
    private String fileId;

    /** 文件名（含扩展名），如 "a1b2c3d4.pdf" */
    private String fileName;

    /** 文件大小（字节） */
    private long fileSize;
}
