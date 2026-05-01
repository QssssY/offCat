package com.airesume.server.mapper;

import com.airesume.server.entity.UserNotification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户站内通知 Mapper
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

}
