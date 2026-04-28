package com.airesume.server.mock;

import com.airesume.server.dto.interview.InterviewJobTargetContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Mock 模拟面试服务。
 * 当未接入真实 AI 时，负责生成可用于联调的开场问题、追问和报告兜底内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockInterviewService {

    private final Random random = new Random();

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
        String difficultyDesc = getDifficultyDesc(difficulty);
        if (context != null && Boolean.TRUE.equals(context.getJobTargeted())) {
            String focusKeyword = pickFirst(context.getMatchedKeywords(), "岗位核心能力");
            String missingKeyword = pickFirst(context.getMissingKeywords(), "关键能力补位");
            return String.format(
                    "你好，欢迎参加本次岗位定向模拟面试。\n\n今天我们聚焦的目标岗位是%s，难度为%s。\n" +
                            "我会结合你的简历经历、目标 JD，以及最近一次岗位对比结果来提问。\n" +
                            "目前我会重点关注你在“%s”方面的真实实践，同时也会追问你如何补足“%s”。\n\n" +
                            "先请你用 2 到 3 分钟介绍一下自己，并重点说明最能支撑该岗位的一段经历。",
                    jobRole,
                    difficultyDesc,
                    focusKeyword,
                    missingKeyword
            );
        }

        return String.format(
                "你好，欢迎参加本次模拟面试。\n\n今天的目标岗位是%s，难度为%s。\n" +
                        "请先做一个简短的自我介绍，并重点说明最近一段与你岗位最相关的项目经历。",
                jobRole,
                difficultyDesc
        );
    }

    /**
     * 生成 Mock 追问。
     */
    public String generateMockReply(String sessionId, String userMessage, int messageIndex, InterviewJobTargetContext context) {
        log.info("Generating mock interview reply, sessionId: {}, messageIndex: {}", sessionId, messageIndex);

        String prefix = GENERIC_REPLIES.get(random.nextInt(GENERIC_REPLIES.size()));
        if (context != null && Boolean.TRUE.equals(context.getJobTargeted())) {
            String targetedQuestion = buildTargetedQuestion(context, messageIndex);
            return prefix + "\n\n" + targetedQuestion;
        }

        String genericQuestion = GENERIC_QUESTIONS.get((messageIndex / 2) % GENERIC_QUESTIONS.size());
        return prefix + "\n\n" + genericQuestion;
    }

    /**
     * 生成兜底分数。
     */
    public int generateMockScore(String sessionId) {
        log.info("Generating mock interview score, sessionId: {}", sessionId);
        return 60 + random.nextInt(35);
    }

    /**
     * 基于岗位定向上下文构造追问。
     */
    private String buildTargetedQuestion(InterviewJobTargetContext context, int messageIndex) {
        List<String> matchedKeywords = context.getMatchedKeywords();
        List<String> missingKeywords = context.getMissingKeywords();
        List<String> suggestions = context.getSuggestions();

        if (messageIndex <= 2 && matchedKeywords != null && !matchedKeywords.isEmpty()) {
            String keyword = matchedKeywords.get(messageIndex % matchedKeywords.size());
            return "请结合一个真实项目，详细说明你是如何把“" + keyword + "”落到业务结果上的？";
        }
        if (missingKeywords != null && !missingKeywords.isEmpty()) {
            String keyword = missingKeywords.get(messageIndex % missingKeywords.size());
            return "目标岗位对“" + keyword + "”有明确要求。请说说你目前的理解，以及如果入职后会如何快速补强？";
        }
        if (suggestions != null && !suggestions.isEmpty()) {
            return "最近一次岗位对比建议你重点优化这类表达：“" + suggestions.get(0) + "”。你会如何在面试中把这部分讲得更有说服力？";
        }
        return "如果让你围绕目标岗位再补充一个最有说服力的案例，你会讲哪一段经历？";
    }

    private String pickFirst(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return values.get(0);
    }

    private String getDifficultyDesc(Integer difficulty) {
        return switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级";
            case 3 -> "高级";
            default -> "中级";
        };
    }
}
