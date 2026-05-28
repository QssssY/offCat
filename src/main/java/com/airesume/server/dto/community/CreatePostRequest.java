package com.airesume.server.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建帖子请求DTO
 */
@Data
public class CreatePostRequest {

    /** 帖子板块：interview_exp 或 referral */
    @NotBlank(message = "帖子板块不能为空")
    private String category;

    /** 帖子标题 */
    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 120, message = "帖子标题不能超过120字")
    private String title;

    /** 帖子内容 */
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 2000, message = "帖子内容不能超过2000字")
    private String content;

    /** 图片URL列表（已上传后的URL） */
    private List<String> images;

    /** 分享到社区的面试报告会话ID；普通帖子为空 */
    @Size(max = 64, message = "面试报告会话ID不能超过64字")
    private String sharedInterviewSessionId;
}
