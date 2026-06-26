package com.airesume.server.service.impl;

import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.mapper.SysGrowthConfigMapper;
import com.airesume.server.service.SysGrowthConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SysGrowthConfigServiceImpl extends ServiceImpl<SysGrowthConfigMapper, SysGrowthConfig>
        implements SysGrowthConfigService {

    @Override
    public List<SysGrowthConfig> getByGroup(String groupName) {
        LambdaQueryWrapper<SysGrowthConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysGrowthConfig::getGroupName, groupName)
                .orderByAsc(SysGrowthConfig::getSort);
        return list(wrapper);
    }

    @Override
    public String getValue(String configKey) {
        LambdaQueryWrapper<SysGrowthConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysGrowthConfig::getConfigKey, configKey);
        SysGrowthConfig config = getOne(wrapper, false);
        return config != null ? config.getConfigValue() : null;
    }
}
