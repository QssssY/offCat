package com.airesume.server.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 简历上传请求DTO
 * 用于接收前端上传的简历文件URL
 */
@Data
public class ResumeUploadRequest {

    /**
     * PDF简历存储地址
     */
    @NotBlank(message = "简历文件地址不能为空")
    private String fileUrl;
}
