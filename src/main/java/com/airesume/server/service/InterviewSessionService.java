package com.airesume.server.service;

import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.entity.InterviewSession;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 面试会话查询服务接口。
 * 这里只保留管理看板和历史查询仍在使用的读能力，写链路统一收口到 InterviewService。
 */
public interface InterviewSessionService extends IService<InterviewSession> {

    /**
     * 根据会话 ID 查询会话详情。
     */
    InterviewSessionResponse getSessionById(String sessionId, Long userId);

    /**
     * 查询用户的面试历史记录。
     */
    List<InterviewHistoryResponse> getHistoryByUserId(Long userId);

    /**
     * 获取状态描述。
     */
    String getStatusDescription(Integer status);

    /**
     * 获取难度描述。
     */
    String getDifficultyDescription(Integer difficulty);
}
