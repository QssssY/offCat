package com.airesume.server.service.impl;

import com.airesume.server.entity.UserRightsChangeLog;
import com.airesume.server.mapper.UserRightsChangeLogMapper;
import com.airesume.server.service.UserRightsChangeLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 管理端用户权益变更日志服务默认实现。
 */
@Service
public class UserRightsChangeLogServiceImpl extends ServiceImpl<UserRightsChangeLogMapper, UserRightsChangeLog>
        implements UserRightsChangeLogService {
}
