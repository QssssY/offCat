package com.airesume.server.service.impl;

import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.mapper.SysVersionLogMapper;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SysVersionLogServiceImpl extends ServiceImpl<SysVersionLogMapper, SysVersionLog>
        implements SysVersionLogService {

    @Override
    // 最新版本日志是首页高频稳定读数据，按安全 limit 缓存，管理端写操作统一驱逐。
    @Cacheable(value = "config:versionLogs", key = "#limit", sync = true)
    public List<SysVersionLog> getLatestPublished(int limit) {
        LambdaQueryWrapper<SysVersionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysVersionLog::getStatus, 1)
                .isNotNull(SysVersionLog::getPublishedAt)
                .orderByDesc(SysVersionLog::getPublishedAt);
        Page<SysVersionLog> page = new Page<>(1, limit);
        Page<SysVersionLog> result = baseMapper.selectPage(page, wrapper);
        return result.getRecords();
    }
}
