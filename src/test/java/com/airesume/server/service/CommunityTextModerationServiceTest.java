package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 社区文本自动审核服务测试。
 * 作用：锁定严重违规自动拒绝、图片进入人工复核、纯文本默认自动通过的分流边界。
 */
class CommunityTextModerationServiceTest {

    private final CommunityTextModerationService moderationService = new CommunityTextModerationService();

    @Test
    void shouldRejectSeverePostTextBeforePersist() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "成人视频资源",
                "这里发布色情成人视频和约炮引流内容。",
                false
        );

        assertTrue(decision.isRejected());
        assertEquals("内容包含违规信息，请修改后再发布", decision.getRejectMessage());
    }

    @Test
    void shouldRouteSuspiciousTextOnlyPostToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "兼职内推",
                "想了解更多可以加薇详聊。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("疑似风险词命中，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldRouteUrlAndPhoneCommentToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewComment(
                "资料在 https://spam.example.com，电话 13812345678。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("疑似风险词命中，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldRouteWechatLikeCommentToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewComment(
                "可以加 vx: resume888 私聊。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("疑似风险词命中，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldRouteSpacedWechatIdWithoutOtherSuspiciousWordsToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewComment(
                "账号 vx: resume888",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("疑似风险词命中，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldRejectSevereImagePostBeforeManualReview() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "面试截图",
                "图片里附带色情成人视频引流内容。",
                true
        );

        assertTrue(decision.isRejected());
        assertEquals("内容包含违规信息，请修改后再发布", decision.getRejectMessage());
    }

    @Test
    void shouldRoutePostWithImagesToPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "面试复盘",
                "这次主要考察了 JVM 和索引。",
                true
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("包含图片，需人工复核", decision.getReviewReason());
    }

    @Test
    void shouldApproveLowRiskTextOnlyPost() {
        CommunityModerationDecision decision = moderationService.reviewPost(
                "Java 面试复盘",
                "这次主要聊了线程池参数、慢 SQL 排查和项目难点。",
                false
        );

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_APPROVED, decision.getReviewStatus());
        assertNull(decision.getReviewReason());
    }

    @Test
    void shouldKeepBlankImageCommentPendingReview() {
        CommunityModerationDecision decision = moderationService.reviewComment("", true);

        assertFalse(decision.isRejected());
        assertEquals(CommunityConstants.REVIEW_STATUS_PENDING, decision.getReviewStatus());
        assertEquals("包含图片，需人工复核", decision.getReviewReason());
    }
}
