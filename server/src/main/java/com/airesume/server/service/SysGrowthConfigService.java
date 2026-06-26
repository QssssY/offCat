package com.airesume.server.service;

import com.airesume.server.entity.SysGrowthConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysGrowthConfigService extends IService<SysGrowthConfig> {

    List<SysGrowthConfig> getByGroup(String groupName);

    String getValue(String configKey);
}
