package com.airesume.server.service;

import com.airesume.server.entity.SysVersionLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysVersionLogService extends IService<SysVersionLog> {

    List<SysVersionLog> getLatestPublished(int limit);
}
