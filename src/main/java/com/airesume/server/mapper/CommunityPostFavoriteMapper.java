package com.airesume.server.mapper;

import com.airesume.server.entity.CommunityPostFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区帖子收藏Mapper接口
 */
@Mapper
public interface CommunityPostFavoriteMapper extends BaseMapper<CommunityPostFavorite> {
}
