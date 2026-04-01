package com.airesume.server.mapper;

import com.airesume.server.entity.InterviewChatLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试聊天记录Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface InterviewChatLogMapper extends BaseMapper<InterviewChatLog> {
}
