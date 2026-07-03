package com.airesume.server.service;

import com.airesume.server.entity.SysJobRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 面试岗位配置服务
 */
public interface SysJobRoleService extends IService<SysJobRole> {

    /**
     * 查询所有岗位配置（管理端使用）。
     *
     * @return 按 sort、id 排序后的岗位列表
     */
    List<SysJobRole> listAllOrdered();

    /**
     * 查询当前启用的岗位配置（用户端使用）。
     *
     * @return 启用岗位列表
     */
    List<SysJobRole> listActiveOrdered();

    /**
     * 根据岗位编码查询岗位配置。
     *
     * @param roleCode 岗位编码
     * @return 岗位配置，不存在返回 null
     */
    SysJobRole getByRoleCode(String roleCode);

    /**
     * 根据岗位名称查询岗位配置。
     *
     * 作用：
     * 用于兼容旧版 Prompt 请求仍然传岗位名称的场景，
     * 但最终仍然会回归到 sys_job_role 中的合法岗位。
     *
     * @param roleName 岗位名称
     * @return 岗位配置，不存在返回 null
     */
    SysJobRole getByRoleName(String roleName);

    /**
     * 判断岗位编码是否已存在。
     *
     * @param roleCode 岗位编码
     * @param excludeId 排除的当前记录 ID，新增时传 null
     * @return 已存在返回 true
     */
    boolean existsByRoleCode(String roleCode, Long excludeId);

    /**
     * 判断岗位名称是否已存在。
     *
     * @param roleName 岗位名称
     * @param excludeId 排除的当前记录 ID，新增时传 null
     * @return 已存在返回 true
     */
    boolean existsByRoleName(String roleName, Long excludeId);

    /**
     * 判断指定岗位名称是否为当前启用岗位。
     *
     * 作用：
     * 创建面试会话时不能只信任前端传参，否则即使前端去掉硬编码，
     * 也仍然可以通过手工构造请求传入无效岗位。
     * 后端必须基于岗位配置表做最终校验。
     *
     * @param roleName 岗位名称
     * @return 启用岗位返回 true
     */
    boolean isActiveRoleName(String roleName);
}
