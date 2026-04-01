package com.airesume.server.mapper;

import com.airesume.server.entity.SysPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI提示词模板Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface SysPromptMapper extends BaseMapper<SysPrompt> {
}
