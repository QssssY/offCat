package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户数据清理结果。
 * deletedCount 只统计本次入口的主记录数量，例如面试会话数或简历诊断任务数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataCleanupResponse {

    /** 本次清理的主记录数量。 */
    private int deletedCount;
}
