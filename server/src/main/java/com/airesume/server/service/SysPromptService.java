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

    /**
     * 根据场景类型+岗位编码+难度获取已启用的Prompt模板
     *
     * @param scenarioType 场景类型
     * @param jobRoleCode  岗位编码
     * @param difficulty  难度级别
     * @return 启用的Prompt实体，未找到时返回null
     */
    SysPrompt getActivePromptByJobRole(Integer scenarioType, String jobRoleCode, Integer difficulty);

    /**
     * 禁用同一场景+岗位+难度的其他已启用Prompt（强制互斥）
     * 在启用一个新Prompt前调用，确保同一岗位+难度组合只有一个启用状态
     *
     * @param scenarioType 场景类型
     * @param jobRoleCode 岗位编码
     * @param difficulty 难度级别
     */
    void deactivateOtherPrompts(Integer scenarioType, String jobRoleCode, Integer difficulty);
}
