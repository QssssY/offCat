package com.airesume.server.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    // === 通用与认证 ===
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(500, "系统错误"),
    BUSINESS_ERROR(500, "业务异常"),

    // === 简历模块 (2xxx) ===
    RESUME_FILE_EMPTY(2001, "上传文件不能为空"),
    RESUME_FORMAT_UNSUPPORTED(2002, "暂不支持该格式，请上传 PDF 文件"),
    RESUME_FILE_TOO_LARGE(2003, "文件大小超出限制"),
    RESUME_PARSE_FAILED(2004, "简历解析失败，请重新上传"),
    RESUME_QUOTA_EXHAUSTED(2005, "今日简历诊断次数已用完"),
    RESUME_FILE_ILLEGAL_PATH(2006, "文件路径不合法"),
    RESUME_TASK_NOT_FOUND(2007, "简历诊断任务不存在"),
    RESUME_TASK_ACCESS_DENIED(2008, "无权访问该简历诊断任务"),
    RESUME_FILE_SAVE_FAILED(2009, "文件保存失败，请稍后重试"),
    RESUME_FILE_CLEANUP_FAILED(2010, "简历文件清理失败"),
    RESUME_TASK_NOT_RETRYABLE(2011, "该任务不可重试"),
    RESUME_TASK_RETRY_EXPIRED(2012, "重试时效已过，请重新上传"),
    POLISH_QUOTA_EXHAUSTED(2013, "今日AI润色次数已用完"),
    POLISH_ALREADY_USED(2014, "该简历已使用过AI润色"),
    JD_MATCH_QUOTA_EXHAUSTED(2015, "今日JD岗位匹配次数已用完"),
    TEMPLATE_QUOTA_EXHAUSTED(2016, "今日模板使用次数已用完"),
    RESUME_STORAGE_SPACE_LOW(2017, "服务器存储空间不足，请稍后再试"),

    // === 面试模块 (3xxx) ===
    INTERVIEW_QUOTA_EXHAUSTED(3001, "今日模拟面试次数已用完"),
    INTERVIEW_SESSION_NOT_FOUND(3002, "面试会话不存在"),
    INTERVIEW_SESSION_ACCESS_DENIED(3003, "无权访问该面试会话"),
    INTERVIEW_SESSION_ENDED(3004, "面试会话已结束，无法继续发送消息"),
    INTERVIEW_AI_TIMEOUT(3005, "AI 生成超时，请稍后重试"),

    // === AI 服务 (4xxx) ===
    AI_SERVICE_UNAVAILABLE(4001, "AI 服务暂时不可用，请稍后重试"),
    AI_RESPONSE_EMPTY(4002, "AI 返回结果为空"),
    AI_RESPONSE_PARSE_FAILED(4003, "AI 响应解析失败"),
    AI_QUOTA_INSUFFICIENT(4004, "AI 调用配额不足"),
    CUSTOM_AI_CALL_FAILED(4090, "自定义AI调用失败"),
    CUSTOM_AI_DAILY_LIMIT_EXCEEDED(4091, "今日自定义AI调用次数已达上限"),
    CUSTOM_AI_CONFIG_INVALID(4092, "自定义AI配置无效"),
    CUSTOM_AI_CONNECTIVITY_FAILED(4093, "AI服务连通测试失败"),

    // === 会员与支付 (5xxx) ===
    MEMBERSHIP_PLAN_NOT_FOUND(5001, "会员套餐不存在或已停用"),
    MEMBERSHIP_ACCOUNT_DISABLED(5002, "账号已被禁用"),
    MEMBERSHIP_USER_NOT_FOUND(5003, "用户不存在"),
    MEMBERSHIP_USER_NOT_LOGGED_IN(5004, "用户未登录"),
    VIP_FEATURE_REQUIRED(5005, "该功能为会员专属，请升级会员后使用"),
    OFFER_QUOTA_EXHAUSTED(5006, "今日Offer辅助次数已用完"),
    FEATURE_QUOTA_EXHAUSTED(5007, "该功能使用次数已达上限"),
    PLAN_DOWNGRADE_NOT_ALLOWED(5008, "已订阅更高级别套餐，无法降级"),

    // === 管理端 (6xxx) ===
    ADMIN_CONFIG_NOT_FOUND(6001, "配置不存在"),
    ADMIN_CODE_DUPLICATE(6002, "编码已存在"),
    ADMIN_BATCH_SIZE_EXCEEDED(6003, "批量操作数量超限"),
    ADMIN_PROMPT_NOT_FOUND(6004, "Prompt 不存在"),
    ADMIN_AI_ENGINE_NOT_FOUND(6005, "AI 引擎配置不存在"),
    ADMIN_JOB_ROLE_NOT_FOUND(6006, "岗位配置不存在");

    private final Integer code;
    private final String message;
}
