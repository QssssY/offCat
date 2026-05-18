package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账号注销请求。
 * 通过当前密码、确认密码和安全问题答案共同校验，避免登录态被盗用时直接触发高风险删除。
 */
@Data
public class AccountDeleteRequest {

    /** 当前登录密码，用于确认账号注销操作本人发起。 */
    @NotBlank(message = "请输入当前密码")
    private String oldPassword;

    /** 确认密码，前后端都校验与当前密码一致，降低误输和脚本误触风险。 */
    @NotBlank(message = "请再次输入当前密码")
    private String confirmPassword;

    /** 安全问题答案，使用用户已设置的 BCrypt 答案进行二次身份验证。 */
    @NotBlank(message = "请输入安全问题答案")
    private String securityAnswer;
}
