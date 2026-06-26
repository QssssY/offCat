package com.airesume.server.service.impl;

import com.airesume.server.entity.InterviewDimensionScore;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.service.InterviewDimensionScoreService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 面试维度评分服务实现。
 * 继承 ServiceImpl 获得 saveBatch 等批量操作能力。
 */
@Service
@RequiredArgsConstructor
public class InterviewDimensionScoreServiceImpl
        extends ServiceImpl<InterviewDimensionScoreMapper, InterviewDimensionScore>
        implements InterviewDimensionScoreService {
}
