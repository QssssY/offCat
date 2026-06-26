package com.airesume.server.mapper;

import com.airesume.server.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 注销账号时匿名化用户主记录并置为逻辑删除。
     * 这里不用物理删除，避免破坏业务表外键和历史审计链路。
     */
    @Update("""
            UPDATE sys_user
            SET username = #{username},
                nickname = #{nickname},
                password = #{password},
                role = 0,
                status = 0,
                membership_plan_code = NULL,
                vip_expire_time = NULL,
                security_question = NULL,
                security_answer = NULL,
                is_deleted = 1,
                update_time = NOW()
            WHERE id = #{userId}
              AND is_deleted = 0
            """)
    int anonymizeDeletedUser(@Param("userId") Long userId,
                             @Param("username") String username,
                             @Param("nickname") String nickname,
                             @Param("password") String password);
}
