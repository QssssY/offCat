package com.airesume.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码ID，提交表单时需要回传 */
    private String captchaId;

    /** Base64 编码的验证码图片，可直接用于 img src */
    private String captchaImage;
}
