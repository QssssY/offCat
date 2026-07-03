package com.airesume.server.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新手任务列表响应
 * 包含全部任务及完成进度，前端根据 visible 判断是否展示卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTasksResponse {

    /** 任务列表 */
    private List<TaskItem> tasks;

    /** 已完成数量 */
    private Integer completedCount;

    /** 总任务数 */
    private Integer totalCount;

    /** 是否全部完成 */
    private Boolean allCompleted;

    /** 是否展示任务卡片（旧用户已完成引导时返回 false） */
    private Boolean visible;

    /** 单个任务项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem {

        /** 任务标识 */
        private String taskKey;

        /** 任务标签 */
        private String taskLabel;

        /** 任务描述 */
        private String taskDesc;

        /** 是否已完成 */
        private Boolean completed;

        /** 行动跳转路由 */
        private String actionUrl;
    }
}
