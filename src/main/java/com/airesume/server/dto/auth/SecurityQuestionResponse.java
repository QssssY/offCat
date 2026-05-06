package com.airesume.server.dto.auth;

import lombok.Data;

/**
 * 返回用户安全问题的响应 DTO
 */
@Data
public class SecurityQuestionResponse {

    /** 安全问题文本 */
    private String securityQuestion;

}
