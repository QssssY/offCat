package com.airesume.server.service.impl;

import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.mapper.SysVersionLogMapper;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SysVersionLogServiceImpl extends ServiceImpl<SysVersionLogMapper, SysVersionLog>
        implements SysVersionLogService {

    @Override
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
