package com.airesume.server.mapper;

import com.airesume.server.entity.CommunityPostLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区帖子点赞Mapper接口
 */
@Mapper
public interface CommunityPostLikeMapper extends BaseMapper<CommunityPostLike> {
}
