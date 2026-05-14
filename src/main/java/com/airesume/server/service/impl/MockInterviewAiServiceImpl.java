package com.airesume.server.service.impl;

import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.service.InterviewAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 模式下的模拟面试 AI 服务实现。
 * 本实现需要与真实 AI 版本保持同一套接口，以保证本地开发也能验证岗位定向链路。
 */
@Service("interviewAiService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.interview.mode", havingValue = "mock", matchIfMissing = true)
public class MockInterviewAiServiceImpl implements InterviewAiService {

    private final MockInterviewService mockInterviewService;
    private final ObjectMapper objectMapper;
    // 使用 ThreadLocalRandom.current() 替代共享 Random 实例，避免多线程竞争

    @Override
    public String generateOpening(String jobRole, String jobRoleCode, Integer difficulty,
                                  InterviewJobTargetContext jobTargetContext) {
        log.info("[MOCK] 生成开场问题, jobRole: {}, difficulty: {}, targeted: {}",
                jobRole, difficulty, jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()));
        return mockInterviewService.generateMockOpening(jobRole, difficulty, jobTargetContext);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage,
                                String jobRoleCode, Integer difficulty, InterviewJobTargetContext jobTargetContext) {
        log.info("[MOCK] 生成面试官回复, sessionId: {}, historySize: {}, targeted: {}",
                sessionId, history == null ? 0 : history.size(),
                jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()));
        int messageCount = history == null ? 0 : history.size();
        return mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext);
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage,
                                                 String jobRoleCode, Integer difficulty,
                                                 InterviewJobTargetContext jobTargetContext) {
        log.info("[MOCK] 流式生成面试官回复, sessionId: {}, historySize: {}, targeted: {}",
                sessionId, history == null ? 0 : history.size(),
                jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()));
        int messageCount = history == null ? 0 : history.size();
        String fullReply = mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext);

        return Flux.<String>create(sink -> {
            for (int i = 0; i < fullReply.length(); i++) {
                sink.next(fullReply.substring(i, i + 1));
                if (Thread.currentThread().isInterrupted()) {
                    sink.error(new RuntimeException("流式输出被中断"));
                    return;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sink.error(new RuntimeException("流式输出被中断"));
                    return;
                }
            }
            sink.complete();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Deprecated
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        log.info("[MOCK] 调用旧版评价接口, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());
        InterviewEvaluationReport report = generateEvaluationReport(
                sessionId, history, "软件工程师", null, 2, "normal", null);
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            return new EvaluationResult(report.getOverallScore(), jsonReport);
        } catch (Exception e) {
            throw new RuntimeException("评价报告序列化失败", e);
        }
    }

    @Override
    public InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            String jobRoleCode,
            Integer difficulty,
            String interviewMode,
            InterviewJobTargetContext jobTargetContext
    ) {
        boolean targeted = jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted());
        log.info("[MOCK] 生成结构化评价报告, sessionId: {}, jobRole: {}, targeted: {}",
                sessionId, jobRole, targeted);

        int baseScore = 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(26);
        int jobMatchScore = targeted ? 65 + java.util.concurrent.ThreadLocalRandom.current().nextInt(21) : 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(16);

        return InterviewEvaluationReport.builder()
                .overallScore(baseScore)
                .level(calculateLevel(baseScore))
                .finalVerdict(buildFinalVerdict(baseScore))
                .summary(buildSummary(baseScore, jobRole, targeted))
                .strengths(buildStrengths(baseScore, jobTargetContext))
                .weaknesses(buildWeaknesses(baseScore, jobTargetContext))
                .criticalIssues(buildCriticalIssues(baseScore))
                .questionPerformance(buildQuestionPerformance(history))
                .technicalDepth(buildDimensionScore(baseScore - 5))
                .communication(buildDimensionScore(baseScore + 5))
                .problemSolving(buildDimensionScore(baseScore - 3))
                .pressureResistance(buildPressureScore(interviewMode, baseScore))
                .jobMatch(buildJobMatchScore(jobMatchScore, targeted, jobTargetContext))
                .hireRecommendation(calculateHireRecommendation(baseScore))
                .improvementSuggestions(buildImprovementSuggestions(baseScore, jobTargetContext))
                .redFlags(buildRedFlags(baseScore))
                .missingCompetencies(buildMissingCompetencies(baseScore, jobTargetContext))
                .inflationRisk(buildInflationRisk(baseScore))
                .answerAuthenticity(buildAnswerAuthenticity(baseScore))
                .interviewPerformanceTags(buildPerformanceTags(baseScore, targeted))
                .passProbability(baseScore)
                .rejectionReasons(buildRejectionReasons(baseScore))
                .build();
    }

    /**
     * 根据分数计算等级。
     */
    private String calculateLevel(int score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 70) {
            return "B";
        }
        if (score >= 60) {
            return "C";
        }
        return "D";
    }

    /**
     * 生成最终录用结论。
     */
    private String buildFinalVerdict(int score) {
        if (score >= 80) {
            return "表现优秀，建议进入下一轮";
        }
        if (score >= 70) {
            return "整体达标，可继续观察";
        }
        if (score >= 60) {
            return "勉强通过，需要结合岗位需求综合判断";
        }
        return "未达到录用门槛，建议继续补强";
    }

    /**
     * 生成整体总结。
     */
    private String buildSummary(int score, String jobRole, boolean targeted) {
        String suffix = targeted ? "，并且对目标岗位要求有一定呼应" : "";
        if (score >= 80) {
            return String.format("本次%s模拟面试表现较好，基础能力和表达都比较稳定%s。", jobRole, suffix);
        }
        if (score >= 70) {
            return String.format("本次%s模拟面试整体达标，但在回答深度和岗位贴合度上还有提升空间%s。", jobRole, suffix);
        }
        if (score >= 60) {
            return String.format("本次%s模拟面试基础尚可，但关键能力展开不够充分%s。", jobRole, suffix);
        }
        return String.format("本次%s模拟面试与目标要求还有明显差距，需要继续补足核心能力%s。", jobRole, suffix);
    }

    /**
     * 生成优势列表。
     */
    private List<String> buildStrengths(int score, InterviewJobTargetContext context) {
        List<String> strengths = new ArrayList<>();
        strengths.add("表达较清晰，回答结构基本完整");
        if (score >= 70) {
            strengths.add("基础知识掌握相对稳定");
        }
        if (score >= 80) {
            strengths.add("能结合项目经历说明自己的思路");
        }
        if (context != null && context.getMatchedKeywords() != null && !context.getMatchedKeywords().isEmpty()) {
            strengths.add("回答中体现了与岗位相关的关键词：" + String.join("、", context.getMatchedKeywords().stream().limit(3).toList()));
        }
        return strengths;
    }

    /**
     * 生成短板列表。
     */
    private List<String> buildWeaknesses(int score, InterviewJobTargetContext context) {
        List<String> weaknesses = new ArrayList<>();
        if (score < 80) {
            weaknesses.add("部分回答停留在概念层，缺少具体细节");
        }
        if (score < 70) {
            weaknesses.add("岗位相关案例展开不够充分");
        }
        if (context != null && context.getMissingKeywords() != null && !context.getMissingKeywords().isEmpty()) {
            weaknesses.add("对目标岗位缺失能力项的回应不够充分：" + String.join("、", context.getMissingKeywords().stream().limit(3).toList()));
        }
        return weaknesses;
    }

    /**
     * 生成严重问题列表。
     */
    private List<String> buildCriticalIssues(int score) {
        List<String> issues = new ArrayList<>();
        if (score < 60) {
            issues.add("关键问题回答偏空泛，缺少实操支撑");
        }
        return issues;
    }

    /**
     * 生成单题表现摘要。
     */
    private List<InterviewEvaluationReport.QuestionPerformance> buildQuestionPerformance(List<ChatMessageItem> history) {
        List<InterviewEvaluationReport.QuestionPerformance> performances = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return performances;
        }

        String currentQuestion = null;
        int questionCount = 0;
        for (ChatMessageItem item : history) {
            if ("assistant".equalsIgnoreCase(item.role()) && questionCount < 3) {
                currentQuestion = item.content();
                continue;
            }
            if (!"user".equalsIgnoreCase(item.role()) || currentQuestion == null || questionCount >= 3) {
                continue;
            }

            int score = 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(26);
            performances.add(InterviewEvaluationReport.QuestionPerformance.builder()
                    .question(trimText(currentQuestion, 100))
                    .answer(trimText(item.content(), 100))
                    .score(score)
                    .comment(score >= 75 ? "回答较完整" : "回答基础达标")
                    .knowledgeTags(List.of("岗位理解", "表达能力"))
                    .build());
            currentQuestion = null;
            questionCount++;
        }
        return performances;
    }

    /**
     * 构建通用维度评分。
     */
    private InterviewEvaluationReport.DimensionScore buildDimensionScore(int baseScore) {
        int score = Math.max(0, Math.min(100, baseScore));
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(score)
                .comment(score >= 80 ? "表现较好" : score >= 60 ? "基本达标" : "需要继续加强")
                .build();
    }

    /**
     * 构建抗压维度评分。
     */
    private InterviewEvaluationReport.DimensionScore buildPressureScore(String interviewMode, int baseScore) {
        int score = "stress".equalsIgnoreCase(interviewMode) ? Math.max(0, baseScore - 10) : baseScore;
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(score)
                .comment("stress".equalsIgnoreCase(interviewMode) ? "压力场景下稳定性一般" : "普通模式下表现正常")
                .build();
    }

    /**
     * 构建岗位匹配评分。
     */
    private InterviewEvaluationReport.DimensionScore buildJobMatchScore(int score, boolean targeted,
                                                                        InterviewJobTargetContext context) {
        String comment;
        if (!targeted) {
            comment = "本次为普通模拟面试，岗位匹配反馈有限";
        } else if (context != null && context.getMissingKeywords() != null && !context.getMissingKeywords().isEmpty()) {
            comment = "回答能覆盖部分岗位要求，但对缺失能力项的回应仍需加强";
        } else {
            comment = "回答与目标岗位要求整体较匹配";
        }
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(Math.max(0, Math.min(100, score)))
                .comment(comment)
                .build();
    }

    /**
     * 计算录用建议。
     */
    private String calculateHireRecommendation(int score) {
        if (score >= 80) {
            return "强烈推荐";
        }
        if (score >= 70) {
            return "推荐";
        }
        if (score >= 60) {
            return "待定";
        }
        return "不推荐";
    }

    /**
     * 生成改进建议。
     */
    private List<String> buildImprovementSuggestions(int score, InterviewJobTargetContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("回答时尽量补充具体项目场景、动作和结果");
        if (score < 80) {
            suggestions.add("加强对核心问题的深入拆解，避免只停留在表面结论");
        }
        if (context != null && context.getSuggestions() != null && !context.getSuggestions().isEmpty()) {
            suggestions.addAll(context.getSuggestions().stream().limit(2).toList());
        }
        return suggestions.stream().distinct().toList();
    }

    /**
     * 生成风险提示。
     */
    private List<String> buildRedFlags(int score) {
        if (score < 50) {
            return List.of("关键岗位能力与目标要求差距较大");
        }
        return new ArrayList<>();
    }

    /**
     * 生成缺失能力列表。
     */
    private List<String> buildMissingCompetencies(int score, InterviewJobTargetContext context) {
        if (context != null && context.getMissingKeywords() != null && !context.getMissingKeywords().isEmpty()) {
            return context.getMissingKeywords().stream().limit(4).toList();
        }
        if (score < 70) {
            return List.of("关键案例沉淀不足", "岗位能力映射不够清晰");
        }
        return new ArrayList<>();
    }

    /**
     * 生成真实性风险说明。
     */
    private String buildInflationRisk(int score) {
        if (score >= 80) {
            return "低 - 回答较自然，可信度较高";
        }
        if (score >= 60) {
            return "中 - 个别回答较泛，需要更多细节支撑";
        }
        return "高 - 关键回答缺少事实依据";
    }

    /**
     * 生成回答真实性说明。
     */
    private String buildAnswerAuthenticity(int score) {
        if (score >= 70) {
            return "可信 - 回答整体符合预期";
        }
        if (score >= 50) {
            return "存疑 - 部分回答不够具体";
        }
        return "不可信 - 多处回答缺少有效细节";
    }

    /**
     * 生成表现标签。
     */
    private List<String> buildPerformanceTags(int score, boolean targeted) {
        List<String> tags = new ArrayList<>();
        tags.add("表达清晰");
        if (score >= 70) {
            tags.add("基础稳定");
        }
        if (targeted) {
            tags.add("岗位定向");
        }
        if (score >= 80) {
            tags.add("案例完整");
        }
        return tags;
    }

    /**
     * 生成不推荐原因。
     */
    private List<String> buildRejectionReasons(int score) {
        if (score < 60) {
            return List.of("关键岗位能力未达到预期", "缺少足够有说服力的项目细节");
        }
        return new ArrayList<>();
    }

    private String trimText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
