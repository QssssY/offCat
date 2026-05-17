package com.airesume.server.service.impl;

import com.airesume.server.entity.SysAdminNotification;
import com.airesume.server.mapper.SysAdminNotificationMapper;
import com.airesume.server.service.SysAdminNotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysAdminNotificationServiceImpl extends ServiceImpl<SysAdminNotificationMapper, SysAdminNotification>
        implements SysAdminNotificationService {
}
