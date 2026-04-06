package com.airesume.server.service;

import com.airesume.server.entity.SysPrompt;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * AI提示词模板服务接口
 * 定义Prompt模板相关的业务操作
 */
public interface SysPromptService extends IService<SysPrompt> {

    /**
     * 根据场景类型查询当前启用的Prompt模板
     *
     * @param scenarioType 场景类型：1-面试系统设定，2-简历诊断设定
     * @return 启用的Prompt内容，未找到时返回null
     */
    String getActivePromptContent(Integer scenarioType);
}
