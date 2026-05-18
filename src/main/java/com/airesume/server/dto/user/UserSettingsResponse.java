package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户设置响应。
 * 0 天表示关闭自动清理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    private Integer interviewRetentionDays;

    private Integer resumeRetentionDays;
}
