package com.airesume.server.service.impl;

import com.airesume.server.common.constants.InterviewConstants;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                                String jobRoleCode, Integer difficulty, InterviewJobTargetContext jobTargetContext,
                                String feedbackMode, String interviewMode, Integer interactionType) {
        log.info("[MOCK] 生成面试官回复, sessionId: {}, historySize: {}, targeted: {}",
                sessionId, history == null ? 0 : history.size(),
                jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()));
        int messageCount = history == null ? 0 : history.size();
        String reply = mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext);
        return applyVoiceInstructionToMockReply(applyStructuredImmediateFeedback(applyPersona(reply, interviewMode), feedbackMode), interactionType);
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage,
                                                 String jobRoleCode, Integer difficulty,
                                                 InterviewJobTargetContext jobTargetContext,
                                                 String feedbackMode, String interviewMode, Integer interactionType) {
        log.info("[MOCK] 流式生成面试官回复, sessionId: {}, historySize: {}, targeted: {}",
                sessionId, history == null ? 0 : history.size(),
                jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()));
        int messageCount = history == null ? 0 : history.size();
        String fullReply = applyVoiceInstructionToMockReply(applyStructuredImmediateFeedback(
                applyPersona(mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext), interviewMode),
                feedbackMode), interactionType);

        // 使用 Flux.interval 按 20ms 节奏吐字，避免在 boundedElastic 线程上 Thread.sleep 造成线程阻塞。
        // 500 字回复在旧实现下会阻塞工作线程约 10 秒，并发面试时容易耗尽 boundedElastic 池。
        return Flux.interval(Duration.ofMillis(20))
                .map(tick -> {
                    int index = tick.intValue();
                    if (index >= fullReply.length()) {
                        return "";
                    }
                    return fullReply.substring(index, index + 1);
                })
                .takeWhile(chunk -> !chunk.isEmpty());
    }

    /**
     * Mock 模式下按固定人设补充语气，保证本地联调能直接看出不同面试官风格。
     */
    private String applyPersona(String reply, String interviewMode) {
        if (com.airesume.server.common.constants.InterviewConstants.MODE_BIG_COMPANY_HR.equalsIgnoreCase(interviewMode)) {
            return "从 HR 面试角度，我会更关注你的动机、协作和行为案例。"
                    + (reply == null ? "" : "\n\n" + reply);
        }
        if (com.airesume.server.common.constants.InterviewConstants.MODE_TECH_LEADER.equalsIgnoreCase(interviewMode)) {
            return "从技术 Leader 角度，我会继续追问技术细节、架构取舍和个人贡献。"
                    + (reply == null ? "" : "\n\n" + reply);
        }
        if (com.airesume.server.common.constants.InterviewConstants.MODE_FOREIGN_INTERVIEWER.equalsIgnoreCase(interviewMode)) {
            return "Let's continue in English. Please keep your answer structured and concise."
                    + (reply == null ? "" : "\n\n" + reply);
        }
        return reply;
    }

    /**
     * Mock 模式下也按即时反馈开关补一段短反馈，便于前端和本地联调直接看到差异。
     */
    private String applyStructuredImmediateFeedback(String reply, String feedbackMode) {
        if (!com.airesume.server.common.constants.InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(feedbackMode)) {
            return reply;
        }
        String safeReply = reply == null ? "" : reply.trim();
        if (safeReply.contains("<FEEDBACK>")) {
            return safeReply;
        }
        return safeReply + "\n\n<FEEDBACK>\n本题反馈：你的回答方向是对的，但案例细节还不够具体，可以补充场景、个人动作和结果。\n</FEEDBACK>";
    }

    /**
     * Mock 模式下也压缩语音面试回复长度，方便本地联调 TTS 逐句播报。
     */
    private String applyVoiceInstructionToMockReply(String reply, Integer interactionType) {
        if (!Integer.valueOf(InterviewConstants.INTERACTION_TYPE_VOICE).equals(interactionType)) {
            return reply;
        }
        String safeReply = reply == null ? "" : reply.trim();
        if (safeReply.isBlank()) {
            return "好的，我们继续。请用一两句话补充你的具体做法。";
        }
        return safeReply.replace("：", "，");
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

        InterviewEvaluationReport.DimensionScore technicalDepth = buildDimensionScore(baseScore - 5);
        InterviewEvaluationReport.DimensionScore projectExpression = buildProjectExpressionScore(baseScore, jobTargetContext);
        InterviewEvaluationReport.DimensionScore communication = buildDimensionScore(baseScore + 5);
        InterviewEvaluationReport.DimensionScore problemSolving = buildDimensionScore(baseScore - 3);
        InterviewEvaluationReport.DimensionScore pressureResistance = buildPressureScore(interviewMode, baseScore);
        InterviewEvaluationReport.DimensionScore jobMatch = buildJobMatchScore(jobMatchScore, targeted, jobTargetContext);

        Map<String, Double> weights = InterviewConstants.getDimensionWeights(difficulty);
        double weightedScore = 0;
        if (technicalDepth.getScore() != null) weightedScore += technicalDepth.getScore() * weights.get("technicalDepth");
        if (projectExpression.getScore() != null) weightedScore += projectExpression.getScore() * weights.get("projectExpression");
        if (communication.getScore() != null) weightedScore += communication.getScore() * weights.get("communication");
        if (problemSolving.getScore() != null) weightedScore += problemSolving.getScore() * weights.get("problemSolving");
        if (pressureResistance.getScore() != null) weightedScore += pressureResistance.getScore() * weights.get("pressureResistance");
        if (jobMatch.getScore() != null) weightedScore += jobMatch.getScore() * weights.get("jobMatch");
        int finalScore = (int) Math.round(weightedScore);

        return InterviewEvaluationReport.builder()
                .overallScore(finalScore)
                .level(calculateLevel(finalScore))
                .finalVerdict(buildFinalVerdict(finalScore))
                .summary(buildSummary(finalScore, jobRole, targeted))
                .strengths(buildStrengths(finalScore, jobTargetContext))
                .weaknesses(buildWeaknesses(finalScore, jobTargetContext))
                .criticalIssues(buildCriticalIssues(finalScore))
                .questionPerformance(buildQuestionPerformance(history))
                .roundReviews(buildRoundReviews(history))
                .followUpLossPoints(buildFollowUpLossPoints(finalScore))
                .commonLossPatterns(buildCommonLossPatterns(finalScore))
                .immediateActions(buildImmediateActions(jobTargetContext))
                .technicalDepth(technicalDepth)
                .projectExpression(projectExpression)
                .communication(communication)
                .problemSolving(problemSolving)
                .pressureResistance(pressureResistance)
                .jobMatch(jobMatch)
                .hireRecommendation(calculateHireRecommendation(finalScore))
                .improvementSuggestions(buildImprovementSuggestions(finalScore, jobTargetContext))
                .redFlags(buildRedFlags(finalScore))
                .missingCompetencies(buildMissingCompetencies(finalScore, jobTargetContext))
                .inflationRisk(buildInflationRisk(finalScore))
                .answerAuthenticity(buildAnswerAuthenticity(finalScore))
                .interviewPerformanceTags(buildPerformanceTags(finalScore, targeted))
                .passProbability(finalScore)
                .rejectionReasons(buildRejectionReasons(finalScore))
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
     * 生成 V2 逐轮复盘。
     * Mock 模式无法做真实语义分析，因此基于问答顺序给出可联调的回放式结构。
     */
    private List<InterviewEvaluationReport.RoundReview> buildRoundReviews(List<ChatMessageItem> history) {
        List<InterviewEvaluationReport.RoundReview> reviews = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return reviews;
        }

        String currentQuestion = null;
        int roundNo = 1;
        for (ChatMessageItem item : history) {
            if ("assistant".equalsIgnoreCase(item.role()) && reviews.size() < 5) {
                currentQuestion = item.content();
                continue;
            }
            if (!"user".equalsIgnoreCase(item.role()) || currentQuestion == null || reviews.size() >= 5) {
                continue;
            }

            int score = 60 + java.util.concurrent.ThreadLocalRandom.current().nextInt(26);
            reviews.add(InterviewEvaluationReport.RoundReview.builder()
                    .roundNo(roundNo)
                    .question(trimText(currentQuestion, 120))
                    .answer(trimText(item.content(), 120))
                    .score(score)
                    .replayAnalysis(score >= 75
                            ? "这一轮回答结构较完整，能够围绕问题给出基本判断，但仍可以补充更多可验证的项目细节。"
                            : "这一轮回答偏概念化，缺少清晰场景、个人动作和结果证据，追问时容易被继续深挖。")
                    .missedFollowUp(score >= 75
                            ? "追问时可以进一步补充边界条件和取舍依据。"
                            : "追问到细节时没有给出足够具体的案例和量化结果。")
                    .nextPractice("用 STAR 结构重写本轮回答，确保包含场景、动作、结果和复盘。")
                    .build());
            currentQuestion = null;
            roundNo++;
        }
        return reviews;
    }

    /**
     * 生成追问失分点，用于前端验证深度报告的追问分析展示。
     */
    private List<String> buildFollowUpLossPoints(int score) {
        List<String> points = new ArrayList<>();
        points.add("被追问实现细节时，回答容易停留在结论，没有展开关键步骤。");
        if (score < 80) {
            points.add("面对连续追问时缺少边界条件说明，容易显得经验沉淀不足。");
        }
        if (score < 70) {
            points.add("追问到项目收益或结果时，缺少可量化证据支撑。");
        }
        return points;
    }

    /**
     * 归纳本次面试的共性失分模式，帮助用户知道优先改哪类问题。
     */
    private List<String> buildCommonLossPatterns(int score) {
        List<String> patterns = new ArrayList<>();
        patterns.add("回答中多次出现抽象判断，缺少具体业务场景。");
        if (score < 80) {
            patterns.add("项目表达没有稳定覆盖背景、动作、结果三要素。");
        }
        if (score < 70) {
            patterns.add("技术问题缺少边界、取舍和失败案例复盘。");
        }
        return patterns;
    }

    /**
     * 生成三条立即行动建议，保持与真实 AI 报告的字段语义一致。
     */
    private List<String> buildImmediateActions(InterviewJobTargetContext context) {
        List<String> actions = new ArrayList<>();
        actions.add("明天选 1 个核心项目，用 STAR 结构写出 2 分钟口述稿并录音复听。");
        actions.add("整理 3 个被追问最多的技术点，每个补充边界条件、失败场景和取舍理由。");
        if (context != null && context.getMissingKeywords() != null && !context.getMissingKeywords().isEmpty()) {
            actions.add("围绕缺失关键词“" + context.getMissingKeywords().get(0) + "”补 1 个项目证据或学习案例。");
        } else {
            actions.add("把本次最低分问题重答一遍，要求回答里至少出现 1 个量化结果。");
        }
        return actions;
    }

    /**
     * 构建项目表达维度评分，单独反映项目案例是否讲清楚。
     */
    private InterviewEvaluationReport.DimensionScore buildProjectExpressionScore(int baseScore,
                                                                                 InterviewJobTargetContext context) {
        int score = Math.max(0, Math.min(100, baseScore - 2));
        List<String> weaknesses = new ArrayList<>();
        if (score < 80) {
            weaknesses.add("项目案例的动作和结果还可以更具体。");
        }
        if (context != null && context.getMissingKeywords() != null && !context.getMissingKeywords().isEmpty()) {
            weaknesses.add("项目表达与目标岗位缺失能力项的映射还不够清晰。");
        }
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(score)
                .comment(score >= 80 ? "项目表达较完整，能支撑能力判断" : "项目表达基本可理解，但证据密度不足")
                .strengths(List.of("能围绕已有项目经历组织回答"))
                .weaknesses(weaknesses)
                .build();
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
