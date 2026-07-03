package com.airesume.server.mapper;

import com.airesume.server.entity.UserRightsChangeLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理端用户权益变更日志数据访问层。
 */
@Mapper
public interface UserRightsChangeLogMapper extends BaseMapper<UserRightsChangeLog> {
}
