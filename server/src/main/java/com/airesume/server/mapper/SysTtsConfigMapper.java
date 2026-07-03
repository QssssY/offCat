package com.airesume.server.mapper;

import com.airesume.server.entity.SysTtsConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统级 TTS 配置 Mapper。
 */
@Mapper
public interface SysTtsConfigMapper extends BaseMapper<SysTtsConfig> {

    /**
     * 查询当前有效单例配置。
     */
    @Select("""
            SELECT *
            FROM sys_tts_config
            WHERE singleton_key = 1
              AND is_deleted = 0
            LIMIT 1
            """)
    SysTtsConfig selectCurrent();

    /**
     * 查询当前启用且未删除的系统 TTS 配置。
     */
    @Select("""
            SELECT *
            FROM sys_tts_config
            WHERE singleton_key = 1
              AND enabled = 1
              AND is_deleted = 0
            LIMIT 1
            """)
    SysTtsConfig selectEnabled();
}
