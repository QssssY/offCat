package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 社区文本自动审核服务。
 * 先用本地规则完成低成本分流：严重违规直接拒绝，图片和疑似广告导流进入人工审核池。
 */
@Service
public class CommunityTextModerationService {

    private static final String REJECT_MESSAGE = "内容包含违规信息，请修改后再发布";
    private static final String IMAGE_REVIEW_REASON = "包含图片，需人工复核";
    private static final String SUSPICIOUS_MARK_REASON = "疑似风险词命中，需人工复核";

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://|www\\.)[a-z0-9.-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern WECHAT_PATTERN = Pattern.compile("(微信|vx|v信|微[\\s_-]*信)\\s*[:：]?\\s*[a-zA-Z][-_a-zA-Z0-9]{5,19}", Pattern.CASE_INSENSITIVE);

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
        // 疑似广告、导流和联系方式不直接公开，先进入人工审核池。
        if (SUSPICIOUS_WORDS.stream().anyMatch(normalized::contains) || hasContactOrUrlPattern(text)) {
            return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_PENDING, SUSPICIOUS_MARK_REASON);
        }
        return CommunityModerationDecision.accepted(CommunityConstants.REVIEW_STATUS_APPROVED, null);
    }

    private boolean hasContactOrUrlPattern(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return URL_PATTERN.matcher(text).find()
                || PHONE_PATTERN.matcher(text).find()
                || WECHAT_PATTERN.matcher(text).find();
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
