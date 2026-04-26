package com.airesume.server.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 批量启用/禁用请求DTO
 */
@Data
public class BatchActiveRequest {
    private List<Long> ids;
    private Integer isActive;
}