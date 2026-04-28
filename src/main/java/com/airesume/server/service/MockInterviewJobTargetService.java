package com.airesume.server.service;

import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewJobTargetedFeedback;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * 根据结构化面试报告构建岗位定向反馈。
     */
    InterviewJobTargetedFeedback buildFeedback(InterviewEvaluationReport report, InterviewJobTargetContext context);

    /**
     * 在面试结束后回写岗位定向反馈。
     */
    void updateFeedback(String sessionId, InterviewJobTargetedFeedback feedback);
}
