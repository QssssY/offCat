package com.airesume.server.mapper;

import com.airesume.server.entity.SysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统 key-value 配置 Mapper。
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
