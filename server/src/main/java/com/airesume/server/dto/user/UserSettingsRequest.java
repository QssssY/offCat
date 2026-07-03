package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户设置保存请求。
 * 保留天数由服务层统一校验，避免前端绕过选项后写入高风险清理策略。
 */
@Data
public class UserSettingsRequest {

    /** 面试记录保留天数为完整保存字段，不能省略；0 表示关闭自动清理。 */
    @NotNull(message = "请选择面试记录保留天数")
    private Integer interviewRetentionDays;

    /** 简历诊断保留天数为完整保存字段，不能省略；0 表示关闭自动清理。 */
    @NotNull(message = "请选择简历诊断保留天数")
    private Integer resumeRetentionDays;
}
