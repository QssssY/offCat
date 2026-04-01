package com.airesume.server.mapper;

import com.airesume.server.entity.ResumeDiagnosisTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历诊断任务Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface ResumeDiagnosisTaskMapper extends BaseMapper<ResumeDiagnosisTask> {
}
