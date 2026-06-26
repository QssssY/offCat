package com.airesume.server.mapper;

import com.airesume.server.entity.UserAiConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户自定义 AI 配置 Mapper。
 */
@Mapper
public interface UserAiConfigMapper extends BaseMapper<UserAiConfig> {

    /**
     * 用户自定义 AI 配置包含敏感密钥，删除时直接物理删除当前用户当前类型配置，
     * 避免逻辑删除记录继续占用唯一键导致后续重复删除失败。
     */
    @Delete("""
            DELETE FROM user_ai_config
            WHERE user_id = #{userId}
              AND config_type = #{configType}
              AND is_deleted = 0
            """)
    int deleteActiveConfig(@Param("userId") Long userId, @Param("configType") String configType);
}
