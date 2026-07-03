package com.airesume.server.service;

import com.airesume.server.dto.auth.CaptchaResponse;

/**
 * 图形验证码服务接口。
 */
public interface CaptchaService {

    /**
     * 生成图形验证码，返回 captchaId 和 Base64 图片。
     */
    CaptchaResponse generate();

    /**
     * 校验验证码，校验成功后删除 Redis 中的验证码（一次性使用）。
     *
     * @param captchaId   验证码ID
     * @param captchaCode 用户输入的验证码
     * @throws com.airesume.server.common.exception.BusinessException 验证码错误或已过期
     */
    void verify(String captchaId, String captchaCode);
}
