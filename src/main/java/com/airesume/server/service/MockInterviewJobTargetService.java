package com.airesume.server.service;

import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewJobTargetedFeedback;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.Map;

/**
 * 岗位定向模拟面试服务接口。
 */
public interface MockInterviewJobTargetService extends IService<MockInterviewJobTargetRecord> {

    /**
     * 在创建会话前解析岗位定向上下文。
     */
    InterviewJobTargetContext resolveContext(Long userId, CreateSessionRequest request);

    /**
     * 在会话创建成功后持久化岗位定向记录。
     */
    void saveSessionContext(Long userId, String sessionId, InterviewJobTargetContext context, String openingQuestion);

    /**
     * 查询指定会话的岗位定向上下文。
     */
    InterviewJobTargetContext getSessionContext(Long userId, String sessionId);

    /**
     * 批量查询历史列表所需的岗位定向摘要，避免逐条查询。
     */
    Map<String, InterviewJobTargetContext> getSessionContextSummaryMap(Long userId, Collection<String> sessionIds);

    /**
     * 查询普通模拟面试可复用的最近简历上下文。
     * 说明：该方法仅返回简历相关信息，不会把普通面试错误标记为岗位定向。
     */
    InterviewJobTargetContext resolveLatestResumeContext(Long userId);

    /**
     * 根据结构化面试报告构建岗位定向反馈。
     */
    InterviewJobTargetedFeedback buildFeedback(InterviewEvaluationReport report, InterviewJobTargetContext context);

    /**
     * 在面试结束后回写岗位定向反馈。
     */
    void updateFeedback(String sessionId, InterviewJobTargetedFeedback feedback);

    /**
     * 逻辑删除当前用户的岗位定向上下文。
     * 用于面试记录清理和账号注销，不物理删除外键关联数据。
     */
    int logicalDeleteByUserId(Long userId);

    /**
     * 按会话批量逻辑删除岗位定向上下文。
     */
    int logicalDeleteBySessionIds(Collection<String> sessionIds);
}
