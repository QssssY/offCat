package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 社区文本自动审核服务。
 * 作用：先用本地规则完成低成本分流，降低管理员处理明显正常纯文本内容的工作量。
 */
@Service
public class CommunityTextModerationService {

    private static final String REJECT_MESSAGE = "内容包含违规信息，请修改后再发布";
    private static final String IMAGE_REVIEW_REASON = "包含图片，需人工复核";
    private static final String SUSPICIOUS_MARK_REASON = "疑似风险词命中，已自动放行";

    private static final List<String> SEVERE_WORDS = List.of(
            "色情", "成人视频", "约炮", "裸聊", "成人视频资源",
            "政治敏感", "反动", "颠覆", "台独", "港独",
            "傻逼", "去死", "诈骗", "博彩"
    );

    private static final List<String> SUSPICIOUS_WORDS = List.of(
            "加微信", "加薇", "私聊", "联系方式", "兼职", "返利", "代办", "资源"
    );

    /**
     * 审核帖子标题和正文。
     */
    public CommunityModerationDecision reviewPost(String title, String content, boolean hasImages) {
        return reviewText(title + "\n" + content, hasImages);
    }

    /**
     * 审核评论正文。
     */
    public CommunityModerationDecision reviewComment(String content, boolean hasImages) {
        return reviewText(content, hasImages);
    }

    private CommunityModerationDecision reviewText(String text, boolean hasImages) {
        String normalized = normalizeModerationText(text);
        if (SEVERE_WORDS.stream().anyMatch(normalized::contains)) {
            return CommunityModerationDecision.rejected(REJECT_MESSAGE);
        }
        if (hasImages) {
            return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_PENDING, IMAGE_REVIEW_REASON);
        }
        // 轻量化审核 V3：疑似词不再压入人工审核，但保留风险标记，方便后续后台筛查和复盘。
        if (SUSPICIOUS_WORDS.stream().anyMatch(normalized::contains)) {
            return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_APPROVED, SUSPICIOUS_MARK_REASON);
        }
        return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_APPROVED, null);
    }

    /**
     * 审核文本归一化，减少空格、大小写和简单符号绕过。
     */
    private String normalizeModerationText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}，。！？、；：“”‘’（）【】《》]", "");
    }
}
