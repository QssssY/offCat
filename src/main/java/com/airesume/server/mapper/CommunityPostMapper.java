package com.airesume.server.mapper;

import com.airesume.server.entity.CommunityPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区帖子Mapper接口
 */
@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {
}
