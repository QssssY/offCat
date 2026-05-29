package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 看板热门岗位排行数据项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotJobRoleResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位名称。
     */
    private String jobRole;

    /**
     * 该岗位的面试会话数。
     */
    private Long sessionCount;
}
