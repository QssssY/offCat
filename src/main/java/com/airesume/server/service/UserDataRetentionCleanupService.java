package com.airesume.server.service;

/**
 * 用户数据保留期自动清理服务。
 * 定时任务通过该服务分批逻辑删除过期记录，避免在用户请求链路中产生额外压力。
 */
public interface UserDataRetentionCleanupService {

    int cleanupExpiredInterviewRecords();

    int cleanupExpiredResumeRecords();
}
