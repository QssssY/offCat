package com.airesume.server.service.impl;

import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.service.InterviewAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.reactivestreams.Publisher;

@Service("interviewAiService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.interview.mode", havingValue = "mock", matchIfMissing = true)
public class MockInterviewAiServiceImpl implements InterviewAiService {

    private final MockInterviewService mockInterviewService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public String generateOpening(String jobRole, String jobRoleCode, Integer difficulty) {
        log.info("[MOCK] 生成面试开场白, jobRole: {}, difficulty: {}", jobRole, difficulty);
        return mockInterviewService.generateMockOpening(jobRole, difficulty);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage, String jobRoleCode, Integer difficulty) {
        log.info("[MOCK] 生成面试官回复, sessionId: {}, historySize: {}, userMessageLength: {}",
                sessionId, history == null ? 0 : history.size(),
                userMessage == null ? 0 : userMessage.length());

        int messageCount = history == null ? 0 : history.size();
        return mockInterviewService.generateMockReply(sessionId, userMessage, messageCount);
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage, String jobRoleCode, Integer difficulty) {
        log.info("[MOCK] 流式生成面试官回复, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());

        int messageCount = history == null ? 0 : history.size();
        String fullReply = mockInterviewService.generateMockReply(sessionId, userMessage, messageCount);

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

    /**
     * 生成面试评价报告（旧版兼容）
     *
     * @deprecated 请使用 generateEvaluationReport 方法
     */
    @Override
    @Deprecated
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        log.info("[MOCK] 调用旧版评价接口, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());
        // 内部转调新方法，保持兼容
        InterviewEvaluationReport report = generateEvaluationReport(
                sessionId, history, "软件工程师", null, 2, "normal");
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            return new EvaluationResult(report.getOverallScore(), jsonReport);
        } catch (Exception e) {
            log.error("[MOCK] 序列化评价报告失败", e);
            throw new RuntimeException("评价报告序列化失败", e);
        }
    }

    /**
     * 生成面试评价报告（新版 Mock 实现）
     *
     * 【说明】
     * 返回结构完整的 Mock 评价报告，字段与真实 AI 评价报告保持一致
     * 方便前端联调和测试
     *
     * @param sessionId     会话 ID
     * @param history       历史消息列表
     * @param jobRole       面试岗位
     * @param difficulty    难度级别
     * @param interviewMode 面试模式
     * @return 结构化评价报告
     */
    @Override
    public InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            String jobRoleCode,
            Integer difficulty,
            String interviewMode
    ) {
        log.info("[MOCK] 生成结构化面试评价, sessionId: {}, jobRole: {}, jobRoleCode: {}, difficulty: {}, mode: {}, historySize: {}",
                sessionId, jobRole, jobRoleCode, difficulty, interviewMode,
                history == null ? 0 : history.size());

        // 生成基础分数（60-85分，Mock 模式下不要太高）
        int baseScore = 60 + random.nextInt(26);

        // 构建结构化评价报告
        InterviewEvaluationReport report = InterviewEvaluationReport.builder()
                .overallScore(baseScore)
                .level(calculateLevel(baseScore))
                .finalVerdict(buildFinalVerdict(baseScore))
                .summary(buildSummary(baseScore, jobRole))
                .strengths(buildStrengths(baseScore))
                .weaknesses(buildWeaknesses(baseScore))
                .criticalIssues(buildCriticalIssues(baseScore))
                .questionPerformance(buildQuestionPerformance(history))
                .technicalDepth(buildDimensionScore(baseScore - 5))
                .communication(buildDimensionScore(baseScore + 5))
                .problemSolving(buildDimensionScore(baseScore - 3))
                .pressureResistance(buildPressureScore(interviewMode, baseScore))
                .jobMatch(buildDimensionScore(baseScore + 2))
                .hireRecommendation(calculateHireRecommendation(baseScore))
                .improvementSuggestions(buildImprovementSuggestions(baseScore))
                .redFlags(buildRedFlags(baseScore))
                .missingCompetencies(buildMissingCompetencies(baseScore))
                .inflationRisk(buildInflationRisk(baseScore))
                .answerAuthenticity(buildAnswerAuthenticity(baseScore))
                .interviewPerformanceTags(buildPerformanceTags(baseScore))
                .passProbability(baseScore)
                .rejectionReasons(buildRejectionReasons(baseScore))
                .build();

        log.info("[MOCK] 结构化评价报告生成完成, overallScore: {}, hireRecommendation: {}",
                report.getOverallScore(), report.getHireRecommendation());

        return report;
    }

    /**
     * 根据分数计算等级
     */
    private String calculateLevel(int score) {
        if (score >= 90) return "S";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        return "D";
    }

    /**
     * 生成最终结论
     */
    private String buildFinalVerdict(int score) {
        if (score >= 80) return "表现优秀，强烈推荐进入下一轮";
        if (score >= 70) return "基本达标，可以考虑进入下一轮";
        if (score >= 60) return "勉强及格，需要综合考量";
        return "未达到录用标准，建议淘汰";
    }

    /**
     * 生成总体评价
     */
    private String buildSummary(int score, String jobRole) {
        if (score >= 80) {
            return String.format("本次面试表现优秀，%s相关技术基础扎实，表达能力强，具有较好的解决问题能力。", jobRole);
        } else if (score >= 70) {
            return String.format("本次面试表现良好，%s相关技术基础较为扎实，能够回答大部分问题，但在某些方面还有提升空间。", jobRole);
        } else if (score >= 60) {
            return String.format("本次面试表现基本合格，具备一定的%s技术基础，但还需要加强系统学习和实践。", jobRole);
        } else {
            return String.format("本次面试还有较大提升空间，建议加强%s基础知识的学习和项目经验的积累。", jobRole);
        }
    }

    /**
     * 生成优势列表
     */
    private List<String> buildStrengths(int score) {
        List<String> strengths = new ArrayList<>();
        strengths.add("表达清晰，逻辑通顺");
        if (score >= 70) {
            strengths.add("基础知识掌握较为扎实");
        }
        if (score >= 80) {
            strengths.add("具有一定的项目经验");
            strengths.add("问题分析能力较强");
        }
        return strengths;
    }

    /**
     * 生成短板列表
     */
    private List<String> buildWeaknesses(int score) {
        List<String> weaknesses = new ArrayList<>();
        if (score < 80) {
            weaknesses.add("部分技术细节理解不够深入");
        }
        if (score < 70) {
            weaknesses.add("系统设计能力有待提升");
        }
        if (score < 60) {
            weaknesses.add("基础知识不够扎实");
        }
        return weaknesses;
    }

    /**
     * 生成严重问题列表
     */
    private List<String> buildCriticalIssues(int score) {
        List<String> issues = new ArrayList<>();
        if (score < 60) {
            issues.add("基础概念混淆严重");
        }
        return issues;
    }

    /**
     * 生成单题表现详情
     */
    private List<InterviewEvaluationReport.QuestionPerformance> buildQuestionPerformance(
            List<ChatMessageItem> history
    ) {
        List<InterviewEvaluationReport.QuestionPerformance> performances = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return performances;
        }

        // 简单记录前3轮对话的表现
        String currentQuestion = null;
        int questionCount = 0;

        for (ChatMessageItem item : history) {
            if ("assistant".equalsIgnoreCase(item.role()) && questionCount < 3) {
                currentQuestion = item.content();
            } else if ("user".equalsIgnoreCase(item.role()) && currentQuestion != null && questionCount < 3) {
                int qScore = 60 + random.nextInt(30);
                List<String> tags = new ArrayList<>();
                tags.add("技术基础");

                performances.add(InterviewEvaluationReport.QuestionPerformance.builder()
                        .question(currentQuestion.length() > 100 ?
                                currentQuestion.substring(0, 100) + "..." : currentQuestion)
                        .answer(item.content().length() > 100 ?
                                item.content().substring(0, 100) + "..." : item.content())
                        .score(qScore)
                        .comment(qScore >= 75 ? "回答较好" : "回答基本合格")
                        .knowledgeTags(tags)
                        .build());

                currentQuestion = null;
                questionCount++;
            }
        }

        return performances;
    }

    /**
     * 构建维度评分
     */
    private InterviewEvaluationReport.DimensionScore buildDimensionScore(int baseScore) {
        // 确保分数在 0-100 范围内
        int score = Math.max(0, Math.min(100, baseScore));
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(score)
                .comment(score >= 80 ? "表现优秀" : score >= 60 ? "基本合格" : "有待提升")
                .build();
    }

    /**
     * 构建抗压能力评分（压力面试模式下评分更低）
     */
    private InterviewEvaluationReport.DimensionScore buildPressureScore(String interviewMode, int baseScore) {
        int score = "stress".equalsIgnoreCase(interviewMode) ?
                Math.max(0, baseScore - 10) : baseScore;
        return InterviewEvaluationReport.DimensionScore.builder()
                .score(score)
                .comment("stress".equalsIgnoreCase(interviewMode) ?
                        "压力面试模式下表现尚可" : "抗压能力正常")
                .build();
    }

    /**
     * 计算录用建议
     */
    private String calculateHireRecommendation(int score) {
        if (score >= 80) return "强烈推荐";
        if (score >= 70) return "推荐";
        if (score >= 60) return "待定";
        return "不推荐";
    }

    /**
     * 生成改进建议
     */
    private List<String> buildImprovementSuggestions(int score) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议深入了解技术底层原理");
        if (score < 80) {
            suggestions.add("建议多学习系统设计相关知识");
        }
        if (score < 70) {
            suggestions.add("建议加强算法和数据结构练习");
        }
        return suggestions;
    }

    /**
     * 生成红旗警示
     */
    private List<String> buildRedFlags(int score) {
        List<String> redFlags = new ArrayList<>();
        if (score < 50) {
            redFlags.add("基础薄弱，需要系统学习");
        }
        return redFlags;
    }

    /**
     * 生成缺失能力列表
     */
    private List<String> buildMissingCompetencies(int score) {
        List<String> competencies = new ArrayList<>();
        if (score < 80) {
            competencies.add("高并发系统设计经验");
        }
        if (score < 70) {
            competencies.add("分布式系统实践经验");
        }
        return competencies;
    }

    /**
     * 生成水分风险评估
     */
    private String buildInflationRisk(int score) {
        if (score >= 80) return "低 - 回答较为真实可信";
        if (score >= 60) return "中 - 部分回答可能存在水分";
        return "高 - 回答真实性存疑";
    }

    /**
     * 生成回答真实性评估
     */
    private String buildAnswerAuthenticity(int score) {
        if (score >= 70) return "可信 - 回答符合预期";
        if (score >= 50) return "存疑 - 部分回答不够具体";
        return "不可信 - 回答过于笼统，缺乏细节";
    }

    /**
     * 生成面试表现标签
     */
    private List<String> buildPerformanceTags(int score) {
        List<String> tags = new ArrayList<>();
        tags.add("表达清晰");
        if (score >= 70) {
            tags.add("基础扎实");
        }
        if (score >= 80) {
            tags.add("逻辑清晰");
            tags.add("项目经验丰富");
        }
        return tags;
    }

    /**
     * 生成拒录理由
     */
    private List<String> buildRejectionReasons(int score) {
        List<String> reasons = new ArrayList<>();
        if (score < 60) {
            reasons.add("技术基础未达到岗位要求");
            reasons.add("项目经验不足");
        }
        return reasons;
    }
}
