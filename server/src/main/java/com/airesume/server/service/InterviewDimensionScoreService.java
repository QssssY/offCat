package com.airesume.server.service;

import com.airesume.server.entity.InterviewDimensionScore;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 面试维度评分服务。
 * 提供批量写入等 IService 标准能力，供 InterviewService 异步报告落库时使用。
 */
public interface InterviewDimensionScoreService extends IService<InterviewDimensionScore> {
}
