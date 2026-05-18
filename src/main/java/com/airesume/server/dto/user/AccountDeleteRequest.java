package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账号注销请求。
 * 通过当前密码二次校验，避免登录态被盗用时直接触发高风险删除。
 */
@Data
public class AccountDeleteRequest {

    /** 当前登录密码，用于确认账号注销操作本人发起。 */
    @NotBlank(message = "请输入当前密码")
    private String oldPassword;
}
