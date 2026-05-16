package com.airesume.server.service;

import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import java.util.List;
import org.reactivestreams.Publisher;

public interface InterviewAiService {

    String generateOpening(String jobRole, String jobRoleCode, Integer difficulty, InterviewJobTargetContext jobTargetContext);

    String generateReply(
            String sessionId,
            List<ChatMessageItem> history,
            String userMessage,
            String jobRoleCode,
            Integer difficulty,
            InterviewJobTargetContext jobTargetContext,
            String feedbackMode,
            String interviewMode
    );

    Publisher<String> generateReplyStream(
            String sessionId,
            List<ChatMessageItem> history,
            String userMessage,
            String jobRoleCode,
            Integer difficulty,
            InterviewJobTargetContext jobTargetContext,
            String feedbackMode,
            String interviewMode
    );

    /**
     * 生成面试评价报告（旧版兼容，返回字符串JSON）
     * @deprecated 请使用 generateEvaluationReport 方法
     */
    @Deprecated
    EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history);

    /**
     * 生成面试评价报告（新版，返回结构化对象）
     * @param sessionId 会话ID
     * @param history 历史消息列表
     * @param jobRole 面试岗位
     * @param jobRoleCode 岗位编码（可为空，用于加载 prompt）
     * @param difficulty 难度级别
     * @param interviewMode 面试模式
     * @return 结构化评价报告
     */
    InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            String jobRoleCode,
            Integer difficulty,
            String interviewMode,
            InterviewJobTargetContext jobTargetContext
    );

    record ChatMessageItem(String role, String content) {}

    record EvaluationResult(int score, String evaluationReport) {}
}
