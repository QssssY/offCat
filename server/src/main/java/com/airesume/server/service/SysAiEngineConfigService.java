package com.airesume.server.service;

import com.airesume.server.entity.SysAiEngineConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 管理端 AI 引擎配置服务。
 */
public interface SysAiEngineConfigService extends IService<SysAiEngineConfig> {

    /**
     * 查询管理端列表所需的全部配置。
     *
     * @return 按规则排序后的配置列表
     */
    List<SysAiEngineConfig> listAllOrdered();

    /**
     * 按稳定引擎编码查询配置。
     *
     * @param engineCode 稳定引擎编码
     * @return 配置对象，不存在返回 null
     */
    SysAiEngineConfig getByEngineCode(String engineCode);

    /**
     * 判断引擎编码是否已存在。
     *
     * @param engineCode 引擎编码
     * @param excludeId 更新时排除的当前记录 ID
     * @return 已重复返回 true
     */
    boolean existsByEngineCode(String engineCode, Long excludeId);

    /**
     * 校验业务类型是否在支持范围内。
     *
     * @param businessType 归一化后的业务类型
     */
    void validateBusinessType(String businessType);

    /**
     * 新增配置，并在同一事务中执行“同业务仅一条启用”规则。
     *
     * @param config 最终入库配置实体
     */
    void saveConfig(SysAiEngineConfig config);

    /**
     * 更新配置，并在同一事务中执行“同业务仅一条启用”规则。
     *
     * @param config 最终更新配置实体
     */
    void updateConfig(SysAiEngineConfig config);

    /**
     * 切换指定配置的启用状态，并保持同业务仅一个启用配置。
     *
     * @param id 配置 ID
     * @param isActive 目标启用状态
     */
    void switchActive(Long id, Integer isActive);

    /**
     * 查询某业务当前启用的配置。
     *
     * @param businessType 归一化后的业务类型
     * @return 启用配置，不存在返回 null
     */
    SysAiEngineConfig getActiveByBusinessType(String businessType);
}
