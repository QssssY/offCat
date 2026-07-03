package com.airesume.server.service.impl;

import com.airesume.server.entity.SysJobRole;
import com.airesume.server.mapper.SysJobRoleMapper;
import com.airesume.server.service.SysJobRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试岗位配置服务实现
 */
@Service
public class SysJobRoleServiceImpl extends ServiceImpl<SysJobRoleMapper, SysJobRole> implements SysJobRoleService {

    private static final int STATUS_ACTIVE = 1;

    @Override
    public List<SysJobRole> listAllOrdered() {
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysJobRole::getSort)
                .orderByAsc(SysJobRole::getId);
        return list(wrapper);
    }

    @Override
    @Cacheable(value = "config:jobRoles", key = "'all'")
    public List<SysJobRole> listActiveOrdered() {
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getIsActive, STATUS_ACTIVE)
                .orderByAsc(SysJobRole::getSort)
                .orderByAsc(SysJobRole::getId);
        return list(wrapper);
    }

    @Override
    public SysJobRole getByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getRoleCode, roleCode.trim())
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    @Override
    public SysJobRole getByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getRoleName, roleName.trim())
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    @Override
    public boolean existsByRoleCode(String roleCode, Long excludeId) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getRoleCode, roleCode.trim());
        if (excludeId != null) {
            wrapper.ne(SysJobRole::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    @Override
    public boolean existsByRoleName(String roleName, Long excludeId) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getRoleName, roleName.trim());
        if (excludeId != null) {
            wrapper.ne(SysJobRole::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    @Override
    public boolean isActiveRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<SysJobRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobRole::getRoleName, roleName.trim())
                .eq(SysJobRole::getIsActive, STATUS_ACTIVE);
        return count(wrapper) > 0;
    }
}
