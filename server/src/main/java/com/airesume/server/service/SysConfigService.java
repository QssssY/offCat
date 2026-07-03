package com.airesume.server.service;

import com.airesume.server.entity.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统 key-value 配置服务。
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 读取用户自定义 AI 每日调用上限。
     */
    int getCustomAiDailyLimit();

    /**
     * 更新用户自定义 AI 每日调用上限。
     */
    void updateCustomAiDailyLimit(int limit);
}
