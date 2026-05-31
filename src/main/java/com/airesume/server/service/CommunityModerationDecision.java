package com.airesume.server.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 社区自动审核决策。
 * 作用：统一表达内容创建时的自动拒绝、自动通过或进入人工复核状态。
 */
@Getter
@RequiredArgsConstructor
public class CommunityModerationDecision {

    private final boolean rejected;
    private final String reviewStatus;
    private final String reviewReason;
    private final String rejectMessage;

    public static CommunityModerationDecision rejected(String message) {
        return new CommunityModerationDecision(true, null, null, message);
    }

    public static CommunityModerationDecision accepted(String reviewStatus, String reviewReason) {
        return new CommunityModerationDecision(false, reviewStatus, reviewReason, null);
    }
}
