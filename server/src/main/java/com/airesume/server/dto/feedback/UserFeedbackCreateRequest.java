package com.airesume.server.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交问题反馈/建议的请求体。
 */
@Data
public class UserFeedbackCreateRequest {

    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "bug|suggestion|experience|other", message = "反馈类型仅支持 bug/suggestion/experience/other")
    private String type;

    @NotBlank(message = "反馈标题不能为空")
    @Size(min = 2, max = 100, message = "反馈标题长度应为 2-100 个字符")
    private String title;

    @NotBlank(message = "反馈内容不能为空")
    @Size(min = 10, max = 2000, message = "反馈内容长度应为 10-2000 个字符")
    private String content;

    @Size(max = 100, message = "联系方式不能超过 100 个字符")
    private String contact;
}
