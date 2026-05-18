package com.airesume.server.service.impl;

import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.GrowthOverviewResponse.*;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.entity.ResumePolishRecord;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.repository.InterviewSessionRepository;
import com.airesume.server.service.GrowthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人成长中心服务实现。
 * 从简历诊断、JD匹配、AI润色、模拟面试等表实时聚合用户成长数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements GrowthService {

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    private final ResumePolishRecordMapper resumePolishRecordMapper;
    private final InterviewSessionRepository interviewSessionRepository;
    private final MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    private final ObjectMapper objectMapper;

    /** 日期格式化：MM/dd */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");

    /** 趋势数据最大条数 */
    private static final int TREND_LIMIT = 10;

    /** 简历诊断任务状态：已完成 */
    private static final int RESUME_STATUS_COMPLETED = 2;

    /** 面试会话状态：已结束 */
    private static final int INTERVIEW_STATUS_ENDED = 1;

    @Override
    @Cacheable(value = "user:growthOverview", key = "#userId")
    public GrowthOverviewResponse getGrowthOverview(Long userId) {
        // 1. 查询简历诊断历史（已完成，按时间倒序）
        List<ResumeDiagnosisTask> resumeTasks = queryCompletedResumeTasks(userId);

        // 2. 查询面试历史（已结束且有评分，按时间倒序）
        List<InterviewSession> interviewSessions = queryCompletedInterviewSessions(userId);

        // 3. 查询最近JD匹配记录
        ResumeJobMatchRecord latestJobMatchRecord = queryLatestJobMatchRecord(userId);

        // 4. 查询最近AI润色记录
        ResumePolishRecord latestPolishRecord = queryLatestPolishRecord(userId);

        // 5. 构建简历分数趋势
        List<ScoreTrendItem> resumeScoreTrend = buildResumeScoreTrend(resumeTasks);

        // 6. 构建面试评分趋势
        List<ScoreTrendItem> interviewScoreTrend = buildInterviewScoreTrend(interviewSessions);

        // 7. 构建成长概览摘要
        SummaryVO summary = buildSummary(userId, resumeTasks, interviewSessions,
                resumeScoreTrend, interviewScoreTrend, latestJobMatchRecord);

        // 8. 构建最近JD匹配结果
        LatestJobMatchVO latestJobMatch = buildLatestJobMatch(latestJobMatchRecord);

        // 9. 构建最近AI润色记录
        LatestPolishVO latestPolish = buildLatestPolish(latestPolishRecord);

        // 10. 构建最近面试反馈
        LatestInterviewFeedbackVO latestInterviewFeedback = buildLatestInterviewFeedback(
                userId, interviewSessions);

        // 11. 构建短板与建议
        WeaknessSummaryVO weaknessSummary = buildWeaknessSummary(
                resumeScoreTrend, interviewScoreTrend, latestJobMatch, latestInterviewFeedback);

        return GrowthOverviewResponse.builder()
                .summary(summary)
                .resumeScoreTrend(resumeScoreTrend)
                .interviewScoreTrend(interviewScoreTrend)
                .latestJobMatch(latestJobMatch)
                .latestPolish(latestPolish)
                .latestInterviewFeedback(latestInterviewFeedback)
                .weaknessSummary(weaknessSummary)
                .build();
    }

    /**
     * 查询用户已完成的简历诊断任务（最近TREND_LIMIT条）
     */
    private List<ResumeDiagnosisTask> queryCompletedResumeTasks(Long userId) {
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getUserId, userId)
                .eq(ResumeDiagnosisTask::getStatus, RESUME_STATUS_COMPLETED)
                .orderByDesc(ResumeDiagnosisTask::getCreateTime)
                .last("limit " + TREND_LIMIT);
        return resumeDiagnosisTaskMapper.selectList(wrapper);
    }

    /**
     * 查询用户已结束且有评分的面试会话（最近TREND_LIMIT条）
     */
    private List<InterviewSession> queryCompletedInterviewSessions(Long userId) {
        return interviewSessionRepository
                .findTop10ByUserIdAndStatusAndComprehensiveScoreIsNotNullAndIsDeletedOrderByCreateTimeDesc(
                        userId, INTERVIEW_STATUS_ENDED, 0);
    }

    /**
     * 查询用户最近一条JD匹配记录
     */
    private ResumeJobMatchRecord queryLatestJobMatchRecord(Long userId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return resumeJobMatchRecordMapper.selectOne(wrapper);
    }

    /**
     * 查询用户最近一条AI润色记录
     */
    private ResumePolishRecord queryLatestPolishRecord(Long userId) {
        LambdaQueryWrapper<ResumePolishRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumePolishRecord::getUserId, userId)
                .orderByDesc(ResumePolishRecord::getCreateTime)
                .last("limit 1");
        return resumePolishRecordMapper.selectOne(wrapper);
    }

    /**
     * 构建简历诊断分数趋势。
     * 从 diagnosis_result JSON 中解析 overallEvaluation.totalScore。
     */
    private List<ScoreTrendItem> buildResumeScoreTrend(List<ResumeDiagnosisTask> tasks) {
        List<ScoreTrendItem> trend = new ArrayList<>();
        // 按时间正序排列（原列表为倒序，需反转）
        List<ResumeDiagnosisTask> sorted = new ArrayList<>(tasks);
        Collections.reverse(sorted);

        for (ResumeDiagnosisTask task : sorted) {
            Integer score = extractResumeScore(task.getDiagnosisResult());
            if (score != null) {
                String date = task.getCreateTime() != null
                        ? task.getCreateTime().format(DATE_FMT) : "";
                trend.add(ScoreTrendItem.builder().date(date).score(score).build());
            }
        }
        return trend;
    }

    /**
     * 从简历诊断结果JSON中提取总分。
     * 路径：overallEvaluation.totalScore
     */
    private Integer extractResumeScore(String diagnosisResultJson) {
        if (diagnosisResultJson == null || diagnosisResultJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(diagnosisResultJson);
            JsonNode totalScore = root.path("overallEvaluation").path("totalScore");
            if (totalScore.isMissingNode() || totalScore.isNull()) {
                return null;
            }
            return totalScore.asInt(0);
        } catch (Exception e) {
            log.warn("[成长中心] 解析简历诊断分数失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建面试评分趋势。
     */
    private List<ScoreTrendItem> buildInterviewScoreTrend(List<InterviewSession> sessions) {
        List<ScoreTrendItem> trend = new ArrayList<>();
        // 按时间正序排列
        List<InterviewSession> sorted = new ArrayList<>(sessions);
        Collections.reverse(sorted);

        for (InterviewSession session : sorted) {
            if (session.getComprehensiveScore() != null) {
                String date = session.getCreateTime() != null
                        ? session.getCreateTime().format(DATE_FMT) : "";
                trend.add(ScoreTrendItem.builder()
                        .date(date)
                        .score(session.getComprehensiveScore())
                        .build());
            }
        }
        return trend;
    }

    /**
     * 构建成长概览摘要。
     */
    private SummaryVO buildSummary(Long userId,
                                    List<ResumeDiagnosisTask> resumeTasks,
                                    List<InterviewSession> interviewSessions,
                                    List<ScoreTrendItem> resumeScoreTrend,
                                    List<ScoreTrendItem> interviewScoreTrend,
                                    ResumeJobMatchRecord latestJobMatchRecord) {
        // 最近简历诊断分数
        Integer latestResumeScore = resumeScoreTrend.isEmpty() ? null
                : resumeScoreTrend.get(resumeScoreTrend.size() - 1).getScore();

        // 最近面试评分
        Integer latestInterviewScore = interviewScoreTrend.isEmpty() ? null
                : interviewScoreTrend.get(interviewScoreTrend.size() - 1).getScore();

        // 最近JD匹配分数（复用已查询的记录）
        Integer latestJobMatchScore = latestJobMatchRecord != null ? latestJobMatchRecord.getMatchScore() : null;

        // 累计简历诊断次数（使用查询到的列表大小，但需要查总数）
        Integer resumeDiagnosisCount = Math.toIntExact(resumeDiagnosisTaskMapper.selectCount(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getUserId, userId)
                        .eq(ResumeDiagnosisTask::getStatus, RESUME_STATUS_COMPLETED)));

        // 累计模拟面试次数
        Integer mockInterviewCount = Math.toIntExact(
                interviewSessionRepository.countByUserIdAndStatus(userId, INTERVIEW_STATUS_ENDED));

        // 累计JD匹配次数
        Integer jobMatchCount = Math.toIntExact(resumeJobMatchRecordMapper.selectCount(
                new LambdaQueryWrapper<ResumeJobMatchRecord>()
                        .eq(ResumeJobMatchRecord::getUserId, userId)));

        // 累计AI润色次数
        Integer polishCount = Math.toIntExact(resumePolishRecordMapper.selectCount(
                new LambdaQueryWrapper<ResumePolishRecord>()
                        .eq(ResumePolishRecord::getUserId, userId)));

        return SummaryVO.builder()
                .latestResumeScore(latestResumeScore)
                .latestInterviewScore(latestInterviewScore)
                .latestJobMatchScore(latestJobMatchScore)
                .resumeDiagnosisCount(resumeDiagnosisCount)
                .mockInterviewCount(mockInterviewCount)
                .jobMatchCount(jobMatchCount)
                .polishCount(polishCount)
                .build();
    }

    /**
     * 构建最近JD匹配结果。
     * 从 analysis_result JSON 中解析匹配分数、关键词和建议。
     */
    private LatestJobMatchVO buildLatestJobMatch(ResumeJobMatchRecord record) {
        if (record == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(record.getAnalysisResult());

            Integer matchScore = record.getMatchScore();
            List<String> matchedKeywords = parseStringList(root.path("matchedKeywords"));
            List<String> missingKeywords = parseStringList(root.path("missingKeywords"));
            List<String> suggestions = parseStringList(root.path("suggestions"));

            String createTime = record.getCreateTime() != null
                    ? record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";

            return LatestJobMatchVO.builder()
                    .matchScore(matchScore)
                    .matchedKeywords(matchedKeywords)
                    .missingKeywords(missingKeywords)
                    .suggestions(suggestions)
                    .createTime(createTime)
                    .build();
        } catch (Exception e) {
            log.warn("[成长中心] 解析JD匹配记录失败: {}", e.getMessage());
            return LatestJobMatchVO.builder()
                    .matchScore(record.getMatchScore())
                    .createTime(record.getCreateTime() != null
                            ? record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                    .build();
        }
    }

    /**
     * 构建最近AI润色记录。
     */
    private LatestPolishVO buildLatestPolish(ResumePolishRecord record) {
        if (record == null) {
            return null;
        }
        List<String> notes = new ArrayList<>();
        try {
            if (record.getModificationNotes() != null && !record.getModificationNotes().isBlank()) {
                JsonNode root = objectMapper.readTree(record.getModificationNotes());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        notes.add(node.asText());
                    }
                } else if (root.isTextual()) {
                    notes.add(root.asText());
                }
            }
        } catch (Exception e) {
            log.warn("[成长中心] 解析润色修改说明失败: {}", e.getMessage());
            // JSON解析失败时，尝试直接作为文本展示
            if (record.getModificationNotes() != null) {
                notes.add(record.getModificationNotes());
            }
        }

        String createTime = record.getCreateTime() != null
                ? record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";

        return LatestPolishVO.builder()
                .sourceType(record.getSourceType())
                .modificationNotes(notes)
                .createTime(createTime)
                .build();
    }

    /**
     * 构建最近面试反馈。
     * 从 evaluation_report JSON 中提取评价摘要，并关联查询岗位定向反馈。
     */
    private LatestInterviewFeedbackVO buildLatestInterviewFeedback(Long userId,
                                                                     List<InterviewSession> sessions) {
        if (sessions.isEmpty()) {
            return null;
        }
        // 取最近一条已结束且有评分的面试
        InterviewSession latest = sessions.get(0);

        // 解析评价报告摘要
        String evaluationSummary = "";
        try {
            if (latest.getEvaluationReport() != null && !latest.getEvaluationReport().isBlank()) {
                JsonNode root = objectMapper.readTree(latest.getEvaluationReport());
                // 提取总结性评价
                if (root.has("summary")) {
                    evaluationSummary = root.get("summary").asText();
                } else if (root.has("overallEvaluation")) {
                    evaluationSummary = root.get("overallEvaluation").asText();
                } else if (root.has("overall")) {
                    evaluationSummary = root.get("overall").asText();
                }
                // 截断过长的摘要
                if (evaluationSummary.length() > 200) {
                    evaluationSummary = evaluationSummary.substring(0, 200) + "...";
                }
            }
        } catch (Exception e) {
            log.warn("[成长中心] 解析面试评价报告失败: {}", e.getMessage());
        }

        // 查询岗位定向反馈
        String jobTargetedFeedbackSummary = "";
        try {
            LambdaQueryWrapper<MockInterviewJobTargetRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MockInterviewJobTargetRecord::getUserId, userId)
                    .eq(MockInterviewJobTargetRecord::getSessionId, latest.getSessionId())
                    .last("limit 1");
            MockInterviewJobTargetRecord targetRecord = mockInterviewJobTargetRecordMapper.selectOne(wrapper);
            if (targetRecord != null && targetRecord.getJobTargetedFeedback() != null
                    && !targetRecord.getJobTargetedFeedback().isBlank()) {
                JsonNode root = objectMapper.readTree(targetRecord.getJobTargetedFeedback());
                if (root.has("summary")) {
                    jobTargetedFeedbackSummary = root.get("summary").asText();
                } else if (root.has("feedback")) {
                    jobTargetedFeedbackSummary = root.get("feedback").asText();
                }
                if (jobTargetedFeedbackSummary.length() > 200) {
                    jobTargetedFeedbackSummary = jobTargetedFeedbackSummary.substring(0, 200) + "...";
                }
            }
        } catch (Exception e) {
            log.warn("[成长中心] 解析岗位定向反馈失败: {}", e.getMessage());
        }

        String createTime = latest.getCreateTime() != null
                ? latest.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";

        return LatestInterviewFeedbackVO.builder()
                .jobRole(latest.getJobRole())
                .interviewMode(latest.getInterviewMode())
                .comprehensiveScore(latest.getComprehensiveScore())
                .evaluationReport(evaluationSummary)
                .jobTargetedFeedback(jobTargetedFeedbackSummary)
                .createTime(createTime)
                .build();
    }

    /**
     * 构建短板与建议。
     * 基于规则聚合已有数据，生成用户当前主要短板和改进建议。
     */
    private WeaknessSummaryVO buildWeaknessSummary(List<ScoreTrendItem> resumeScoreTrend,
                                                    List<ScoreTrendItem> interviewScoreTrend,
                                                    LatestJobMatchVO latestJobMatch,
                                                    LatestInterviewFeedbackVO latestInterviewFeedback) {
        List<String> resumeWeaknesses = new ArrayList<>();
        List<String> jobMatchWeaknesses = new ArrayList<>();
        List<String> interviewWeaknesses = new ArrayList<>();
        LinkedHashSet<String> suggestionsSet = new LinkedHashSet<>();

        // 简历分数规则
        if (!resumeScoreTrend.isEmpty()) {
            int latestScore = resumeScoreTrend.get(resumeScoreTrend.size() - 1).getScore();
            if (latestScore < 60) {
                resumeWeaknesses.add("简历整体质量较低，建议进行深度优化");
                suggestionsSet.add("优化简历结构、项目描述和量化成果");
            } else if (latestScore < 70) {
                resumeWeaknesses.add("简历质量有提升空间");
            }
            // 趋势规则
            if (resumeScoreTrend.size() >= 2) {
                int firstScore = resumeScoreTrend.get(0).getScore();
                if (latestScore < firstScore - 5) {
                    resumeWeaknesses.add("简历分数呈下降趋势，建议回顾近期修改是否合理");
                }
            }
        }

        // 面试分数规则
        if (!interviewScoreTrend.isEmpty()) {
            int latestScore = interviewScoreTrend.get(interviewScoreTrend.size() - 1).getScore();
            if (latestScore < 50) {
                interviewWeaknesses.add("面试表现较弱，需要系统性提升");
                suggestionsSet.add("加强项目表达、技术原理理解和问题复盘能力");
            } else if (latestScore < 70) {
                interviewWeaknesses.add("面试技巧需要进一步打磨");
            }
            // 趋势规则
            if (interviewScoreTrend.size() >= 2) {
                int firstScore = interviewScoreTrend.get(0).getScore();
                if (latestScore < firstScore - 5) {
                    interviewWeaknesses.add("面试表现呈下降趋势，建议针对性复盘");
                }
            }
        }

        // JD匹配规则
        if (latestJobMatch != null) {
            if (latestJobMatch.getMatchScore() != null && latestJobMatch.getMatchScore() < 50) {
                jobMatchWeaknesses.add("简历与目标岗位匹配度较低");
            }
            // 缺失关键词转建议
            if (latestJobMatch.getMissingKeywords() != null) {
                latestJobMatch.getMissingKeywords().stream()
                        .limit(3)
                        .forEach(kw -> suggestionsSet.add("建议在简历中补充「" + kw + "」相关经历"));
            }
        }

        // 面试报告建议
        if (latestInterviewFeedback != null) {
            if (latestInterviewFeedback.getEvaluationReport() != null
                    && !latestInterviewFeedback.getEvaluationReport().isBlank()) {
                suggestionsSet.add("回顾最近一次面试报告，针对性改进");
            }
        }

        // 截断建议列表，最多5条
        List<String> suggestions = suggestionsSet.stream()
                .limit(5)
                .collect(Collectors.toList());

        return WeaknessSummaryVO.builder()
                .resumeWeaknesses(resumeWeaknesses)
                .jobMatchWeaknesses(jobMatchWeaknesses)
                .interviewWeaknesses(interviewWeaknesses)
                .suggestions(suggestions)
                .build();
    }

    /**
     * 从JSON数组节点解析为字符串列表。
     */
    private List<String> parseStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                result.add(node.asText());
            }
        }
        return result;
    }
}
