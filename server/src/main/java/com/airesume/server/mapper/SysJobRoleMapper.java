package com.airesume.server.mapper;

import com.airesume.server.entity.SysJobRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 面试岗位配置 Mapper
 */
@Mapper
public interface SysJobRoleMapper extends BaseMapper<SysJobRole> {

    /**
     * 锁定岗位配置行，作为 Prompt 互斥更新的事务锚点。
     * 这样即使当前 Prompt 组里还没有任何记录，也能串行化同岗位下的启用操作。
     */
    @Select("""
            SELECT id
            FROM sys_job_role
            WHERE role_code = #{roleCode}
            LIMIT 1
            FOR UPDATE
            """)
    Long lockIdByRoleCode(@Param("roleCode") String roleCode);
}
