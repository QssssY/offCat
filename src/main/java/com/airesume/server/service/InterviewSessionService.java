package com.airesume.server.service;

import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewSession;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 面试会话服务接口
 * 定义面试相关的业务操作
 */
public interface InterviewSessionService extends IService<InterviewSession> {

    /**
     * 创建面试会话
     *
     * @param userId   用户ID
     * @param jobRole  面试岗位
     * @param difficulty 难度级别
     * @return 会话ID
     */
    String createSession(Long userId, String jobRole, Integer difficulty);

    /**
     * 发送面试消息并获取回复
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param content   用户消息内容
     * @return 面试官回复
     */
    SendMessageResponse sendMessage(String sessionId, Long userId, String content);

    /**
     * 结束面试
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    void endInterview(String sessionId, Long userId);

    /**
     * 根据会话ID查询会话详情
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于校验权限）
     * @return 会话详情响应
     */
    InterviewSessionResponse getSessionById(String sessionId, Long userId);

    /**
     * 查询用户的面试历史记录
     *
     * @param userId 用户ID
     * @return 历史记录列表
     */
    List<InterviewHistoryResponse> getHistoryByUserId(Long userId);

    /**
     * 获取状态描述
     *
     * @param status 状态码
     * @return 状态描述
     */
    String getStatusDescription(Integer status);

    /**
     * 获取难度描述
     *
     * @param difficulty 难度级别
     * @return 难度描述
     */
    String getDifficultyDescription(Integer difficulty);
}
