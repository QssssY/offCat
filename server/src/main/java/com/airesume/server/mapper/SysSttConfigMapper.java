package com.airesume.server.mapper;

import com.airesume.server.entity.SysSttConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统级 STT 配置 Mapper。
 */
@Mapper
public interface SysSttConfigMapper extends BaseMapper<SysSttConfig> {

    /**
     * 查询当前有效单例配置。
     */
    @Select("""
            SELECT *
            FROM sys_stt_config
            WHERE singleton_key = 1
              AND is_deleted = 0
            LIMIT 1
            """)
    SysSttConfig selectCurrent();

    /**
     * 查询当前启用且未删除的系统 STT 配置。
     */
    @Select("""
            SELECT *
            FROM sys_stt_config
            WHERE singleton_key = 1
              AND enabled = 1
              AND is_deleted = 0
            LIMIT 1
            """)
    SysSttConfig selectEnabled();
}
