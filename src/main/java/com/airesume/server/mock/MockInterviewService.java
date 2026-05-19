package com.airesume.server.mock;

import com.airesume.server.dto.interview.InterviewJobTargetContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Mock 模拟面试服务。
 * 当未接入真实 AI 时，负责生成可用于联调的开场问题、追问和报告兜底内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockInterviewService {

    // 使用 ThreadLocalRandom.current() 替代共享 Random 实例，避免多线程竞争

    /**
     * 通用追问模板。
     */
    private static final List<String> GENERIC_REPLIES = Arrays.asList(
            "我理解了。请继续结合你自己的实际项目说明一下。",
            "这个回答还可以，再往深一点讲讲你的关键决策过程。",
            "如果放到真实业务场景里，你会如何落地这件事？",
            "这个方向不错。你能补充一下结果数据或业务影响吗？"
    );

    /**
     * 通用问题模板。
     */
    private static final List<String> GENERIC_QUESTIONS = Arrays.asList(
            "请先做一个简短的自我介绍，并重点说明与你申请岗位最相关的经历。",
            "请讲一个你最近主导或深度参与的项目，重点说清楚你的职责和成果。",
            "当你在项目中遇到复杂问题时，通常如何定位并推动解决？",
            "如果让你重新做一次这个项目，你最想优化的部分是什么？"
    );

    /**
     * 生成 Mock 开场问题。
     */
    public String generateMockOpening(String jobRole, Integer difficulty, InterviewJobTargetContext context) {
        boolean jobTargeted = context != null && Boolean.TRUE.equals(context.getJobTargeted());
        boolean hasResume = hasResumeText(context);
        boolean hasJd = context != null && context.getJdText() != null && !context.getJdText().isBlank();
        String resumeTopic = pickResumeTopic(context == null ? null : context.getResumeText());

        if (jobTargeted && hasResume && hasJd) {
            String missingKeyword = pickFirst(context.getMissingKeywords(), pickFirst(context.getMatchedKeywords(), "目标岗位核心能力"));
            return String.format("我看到你简历里有一段%s。如果把这段经历放到%s岗位场景里，你会怎样证明自己具备“%s”这项能力？",
                    resumeTopic,
                    jobRole,
                    missingKeyword);
        }
        if (jobTargeted && hasJd) {
            String focusKeyword = pickFirst(context.getMatchedKeywords(), pickFirst(context.getMissingKeywords(), "岗位核心要求"));
            return String.format("这次我们按%s岗位的 JD 来聊。JD 里重点提到了“%s”，你先说说你对这项能力的理解，以及会如何在真实工作中落地？",
                    jobRole,
                    focusKeyword);
        }
        if (hasResume) {
            return String.format("我看到你简历里有一段%s。你当时具体负责了什么，过程中最能体现你%s岗位能力的一个关键决策是什么？",
                    resumeTopic,
                    jobRole);
        }

        return GENERIC_QUESTIONS.get(0);
    }

    /**
     * 生成 Mock 追问。
     */
    public String generateMockReply(String sessionId, String userMessage, int messageIndex, InterviewJobTargetContext context) {
        log.info("Generating mock interview reply, sessionId: {}, messageIndex: {}", sessionId, messageIndex);

        if (context != null && Boolean.TRUE.equals(context.getJobTargeted())) {
            return buildTargetedQuestion(context, messageIndex);
        }

        if (hasResumeText(context)) {
            return buildResumeBasedQuestion(context, messageIndex);
        }

        String genericQuestion = GENERIC_QUESTIONS.get((messageIndex / 2) % GENERIC_QUESTIONS.size());
        return genericQuestion;
    }

    /**
     * 生成兜底分数。
     */
    public int generateMockScore(String sessionId) {
        log.info("Generating mock interview score, sessionId: {}", sessionId);
        return 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(35);
    }

    /**
     * 基于岗位定向上下文构造追问。
     */
    private String buildTargetedQuestion(InterviewJobTargetContext context, int messageIndex) {
        String resumeTopic = pickResumeTopic(context.getResumeText());
        List<String> matchedKeywords = context.getMatchedKeywords();
        List<String> missingKeywords = context.getMissingKeywords();
        List<String> suggestions = context.getSuggestions();

        if (hasResumeText(context) && matchedKeywords != null && !matchedKeywords.isEmpty()) {
            String keyword = matchedKeywords.get(messageIndex % matchedKeywords.size());
            return "继续追问你简历里的这段" + resumeTopic + "，你当时是怎么把“" + keyword + "”真正落到结果上的？";
        }
        if (hasResumeText(context) && missingKeywords != null && !missingKeywords.isEmpty()) {
            String keyword = missingKeywords.get(messageIndex % missingKeywords.size());
            return "如果把你简历里的这段" + resumeTopic + "迁移到目标岗位场景，面对“" + keyword + "”这项要求，你会怎样补足能力缺口？";
        }
        if (missingKeywords != null && !missingKeywords.isEmpty()) {
            String keyword = missingKeywords.get(messageIndex % missingKeywords.size());
            return "目标岗位对“" + keyword + "”有明确要求。你会如何在真实项目里验证自己具备这项能力？";
        }
        if (suggestions != null && !suggestions.isEmpty()) {
            return "如果你要把这段经历讲得更贴近目标岗位，你会怎么回应这条建议：“" + suggestions.get(0) + "”？";
        }
        if (hasResumeText(context)) {
            return "如果继续围绕你简历里的这段经历展开，你觉得哪一部分最能证明你适合目标岗位？";
        }
        return "如果继续围绕目标岗位深入一轮，你会先举哪个最能证明自己能力的案例？";
    }

    /**
     * 普通模拟面试在有简历上下文时，也要从简历经历切入，不再退回泛泛提问。
     */
    private String buildResumeBasedQuestion(InterviewJobTargetContext context, int messageIndex) {
        String resumeTopic = pickResumeTopic(context.getResumeText());
        return switch (messageIndex % 4) {
            case 0 -> "回到你简历里的这段" + resumeTopic + "，你当时承担的核心职责和结果指标分别是什么？";
            case 1 -> "在这段" + resumeTopic + "里，你做过的一个关键技术或方案选择是什么，为什么这么定？";
            case 2 -> "如果复盘这段" + resumeTopic + "，你认为当时最难解决的问题是什么，你是怎么处理的？";
            default -> "你再讲一个这段" + resumeTopic + "里最能体现个人价值的细节。";
        };
    }

    private boolean hasResumeText(InterviewJobTargetContext context) {
        return context != null && context.getResumeText() != null && !context.getResumeText().isBlank();
    }

    /**
     * 从简历文本里判断适合追问的经历类型。
     * Mock 问题不能直接展示简历原文、文件名或姓名性别等元信息，只保留泛化经历类型。
     */
    private String pickResumeTopic(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return "相关经历";
        }
        String normalized = resumeText.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.contains("实习")) {
            return "实习经历";
        }
        if (normalized.contains("项目") || normalized.contains("系统")
                || normalized.contains("平台") || normalized.contains("商城")) {
            return "项目经历";
        }
        if (normalized.contains("工作") || normalized.contains("任职")) {
            return "工作经历";
        }
        return "相关经历";
    }

    private String pickFirst(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return values.get(0);
    }

    private String getDifficultyDesc(Integer difficulty) {
        return com.airesume.server.common.constants.InterviewConstants.getDifficultyLabel(difficulty == null ? 2 : difficulty);
    }
}
