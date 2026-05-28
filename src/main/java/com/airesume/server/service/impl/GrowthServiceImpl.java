package com.airesume.server.service.impl;

import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.GrowthOverviewResponse.*;
import com.airesume.server.dto.growth.InterviewRadarResponse;
import com.airesume.server.dto.growth.InterviewRadarResponse.*;
import com.airesume.server.entity.InterviewDimensionScore;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.entity.ResumePolishRecord;
import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.service.GrowthService;
import com.airesume.server.service.SysGrowthConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final InterviewSessionMapper interviewSessionMapper;
    private final MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    private final InterviewDimensionScoreMapper dimensionScoreMapper;
    private final SysGrowthConfigService sysGrowthConfigService;
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

        // 12. 读取管理端成长配置，驱动用户端激励文案和里程碑展示
        GrowthConfigVO growthConfig = buildGrowthConfig();

        return GrowthOverviewResponse.builder()
                .summary(summary)
                .resumeScoreTrend(resumeScoreTrend)
                .interviewScoreTrend(interviewScoreTrend)
                .latestJobMatch(latestJobMatch)
                .latestPolish(latestPolish)
                .latestInterviewFeedback(latestInterviewFeedback)
                .weaknessSummary(weaknessSummary)
                .growthConfig(growthConfig)
                .build();
    }

    /**
     * 查询用户已完成的简历诊断任务（最近TREND_LIMIT条）
     */
    private List<ResumeDiagnosisTask> queryCompletedResumeTasks(Long userId) {
        QueryWrapper<ResumeDiagnosisTask> wrapper = new QueryWrapper<>();
        // 成长趋势只解析诊断分数和时间，避免加载 resume_text 等大文本字段。
        wrapper.select("create_time", "diagnosis_result")
                .eq("user_id", userId)
                .eq("status", RESUME_STATUS_COMPLETED)
                .orderByDesc("create_time")
                .last("limit " + TREND_LIMIT);
        return resumeDiagnosisTaskMapper.selectList(wrapper);
    }

    /**
     * 查询用户已结束且有评分的面试会话（最近TREND_LIMIT条）
     */
    private List<InterviewSession> queryCompletedInterviewSessions(Long userId) {
        QueryWrapper<InterviewSession> wrapper = new QueryWrapper<>();
        // 成长趋势和最近反馈共用最近 10 条；为保持原返回结构，显式补回 evaluation_report。
        wrapper.select("id", "session_id", "user_id", "job_role", "interview_mode",
                        "status", "comprehensive_score", "evaluation_report", "create_time", "update_time")
                .eq("user_id", userId)
                .eq("status", INTERVIEW_STATUS_ENDED)
                .eq("is_deleted", 0)
                .isNotNull("comprehensive_score")
                .orderByDesc("create_time")
                .last("limit " + TREND_LIMIT);
        return interviewSessionMapper.selectList(wrapper);
    }

    /**
     * 查询最近有评估报告的面试会话，雷达图依赖 evaluation_report 中的维度明细而不是综合分。
     */
    private List<InterviewSession> queryInterviewSessionsWithEvaluationReport(Long userId) {
        QueryWrapper<InterviewSession> wrapper = new QueryWrapper<>();
        // 雷达图依赖 evaluation_report，仅该读路径显式补回。
        wrapper.select("id", "session_id", "user_id", "job_role", "interview_mode",
                        "status", "comprehensive_score", "evaluation_report", "create_time", "update_time")
                .eq("user_id", userId)
                .eq("status", INTERVIEW_STATUS_ENDED)
                .eq("is_deleted", 0)
                .isNotNull("evaluation_report")
                .ne("evaluation_report", "")
                .orderByDesc("create_time")
                .last("limit " + TREND_LIMIT);
        return interviewSessionMapper.selectList(wrapper);
    }

    /**
     * 查询用户最近一条JD匹配记录
     */
    private ResumeJobMatchRecord queryLatestJobMatchRecord(Long userId) {
        QueryWrapper<ResumeJobMatchRecord> wrapper = new QueryWrapper<>();
        // 成长概览只需要分数、分析 JSON 和创建时间，避免加载简历/JD 文本快照。
        wrapper.select("match_score", "analysis_result", "create_time")
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .last("limit 1");
        return resumeJobMatchRecordMapper.selectOne(wrapper);
    }

    /**
     * 查询用户最近一条AI润色记录
     */
    private ResumePolishRecord queryLatestPolishRecord(Long userId) {
        QueryWrapper<ResumePolishRecord> wrapper = new QueryWrapper<>();
        // 最近润色卡片只展示来源、修改说明和时间，避免加载润色全文与结构化文档 JSON。
        wrapper.select("source_type", "modification_notes", "create_time")
                .eq("user_id", userId)
                .orderByDesc("create_time")
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
                interviewSessionMapper.selectCount(new QueryWrapper<InterviewSession>()
                        .eq("user_id", userId)
                        .eq("status", INTERVIEW_STATUS_ENDED)
                        .eq("is_deleted", 0)));

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
                    // 最近面试反馈需要岗位定向反馈 JSON。
                    .select(MockInterviewJobTargetRecord::getId,
                            MockInterviewJobTargetRecord::getUserId,
                            MockInterviewJobTargetRecord::getSessionId,
                            MockInterviewJobTargetRecord::getJobTargetedFeedback)
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

    /**
     * 构建管理端配置驱动的成长中心展示内容。
     * encouragement 分组只取配置值作为用户端文案，milestone 分组保留 key/说明/排序供前端稳定渲染。
     */
    private GrowthConfigVO buildGrowthConfig() {
        List<String> encouragementMessages = sysGrowthConfigService.getByGroup("encouragement").stream()
                .map(SysGrowthConfig::getConfigValue)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());

        List<MilestoneConfigVO> milestones = sysGrowthConfigService.getByGroup("milestone").stream()
                .filter(config -> config.getConfigValue() != null && !config.getConfigValue().isBlank())
                .map(config -> MilestoneConfigVO.builder()
                        .configKey(config.getConfigKey())
                        .title(config.getConfigValue().trim())
                        .description(config.getDescription())
                        .sort(config.getSort())
                        .build())
                .collect(Collectors.toList());

        return GrowthConfigVO.builder()
                .encouragementMessages(encouragementMessages)
                .milestones(milestones)
                .build();
    }

    // ==================== 面试维度雷达相关 ====================

    /** 6 维度标识 → 中文标签 */
    private static final Map<String, String> DIMENSION_LABELS = Map.of(
            "technicalDepth", "技术深度",
            "projectExpression", "项目表达",
            "communication", "沟通表达",
            "problemSolving", "问题解决",
            "pressureResistance", "抗压表现",
            "jobMatch", "岗位匹配"
    );

    /** 维度顺序（雷达图按此顺序渲染） */
    private static final List<String> DIMENSION_KEYS = List.of(
            "technicalDepth", "projectExpression", "communication",
            "problemSolving", "pressureResistance", "jobMatch"
    );

    /** 盲区分析取最近 N 次面试 */
    private static final int BLIND_SPOT_SESSION_COUNT = 3;

    /** 持续低分阈值 */
    private static final int PERSISTENT_LOW_THRESHOLD = 60;

    /** 下降趋势：最近分数阈值 */
    private static final int DECLINING_SCORE_THRESHOLD = 70;

    /** 下降趋势：下降幅度阈值 */
    private static final int DECLINING_DROP_THRESHOLD = 5;

    /** 维度改进建议文案 */
    private static final Map<String, List<String>> DIMENSION_SUGGESTIONS = Map.of(
            "technicalDepth", List.of("深入学习核心技术原理，不停留在 API 使用层面", "准备技术深挖题，能从源码和设计层面回答"),
            "projectExpression", List.of("用 STAR 法则重新梳理项目经历", "量化项目成果，突出个人贡献和技术难点"),
            "communication", List.of("练习结构化表达，先说结论再展开", "控制回答时长，避免跑题和冗余描述"),
            "problemSolving", List.of("多练习算法和系统设计题", "培养拆解问题的习惯，展示分析过程"),
            "pressureResistance", List.of("模拟压力面试场景进行脱敏训练", "学会在压力下保持逻辑清晰和情绪稳定"),
            "jobMatch", List.of("研究目标岗位 JD，针对性准备匹配经历", "突出与岗位要求对口的技能和项目经验")
    );

    @Override
    @Cacheable(value = "user:interviewRadar", key = "#userId")
    public InterviewRadarResponse getInterviewRadar(Long userId) {
        // 1. 查询最近已结束且有评估报告的面试会话
        List<InterviewSession> sessions = queryInterviewSessionsWithEvaluationReport(userId);
        if (sessions.isEmpty()) {
            return InterviewRadarResponse.builder()
                    .sessionCount(0)
                    .dimensionTrends(Collections.emptyList())
                    .blindSpotTips(Collections.emptyList())
                    .build();
        }

        // 2. 查询所有相关 session 的维度评分记录；雷达读路径只读库，不在缓存读取中做回填写入。
        List<String> sessionIds = sessions.stream()
                .map(InterviewSession::getSessionId)
                .collect(Collectors.toList());
        List<InterviewDimensionScore> allScores = dimensionScoreMapper.selectList(
                new LambdaQueryWrapper<InterviewDimensionScore>()
                        .in(InterviewDimensionScore::getSessionId, sessionIds)
                        .eq(InterviewDimensionScore::getIsDeleted, 0)
                        .orderByAsc(InterviewDimensionScore::getCreateTime));

        // 按 sessionId 分组
        Map<String, List<InterviewDimensionScore>> scoresBySession = allScores.stream()
                .collect(Collectors.groupingBy(InterviewDimensionScore::getSessionId));

        // 仅保留有维度评分的 session（按原顺序，最新在前）
        List<InterviewSession> scoredSessions = sessions.stream()
                .filter(s -> scoresBySession.containsKey(s.getSessionId()))
                .collect(Collectors.toList());

        if (scoredSessions.isEmpty()) {
            return InterviewRadarResponse.builder()
                    .sessionCount(sessions.size())
                    .dimensionTrends(Collections.emptyList())
                    .blindSpotTips(Collections.emptyList())
                    .build();
        }

        // 4. 构建最新一次雷达数据
        RadarDataVO latestRadar = buildLatestRadar(scoredSessions.get(0), scoresBySession);

        // 5. 构建维度趋势（按时间正序）
        List<DimensionTrendVO> dimensionTrends = buildDimensionTrends(scoredSessions, scoresBySession);

        // 6. 盲区分析
        List<BlindSpotTipVO> blindSpotTips = analyzeBlindSpots(scoredSessions, scoresBySession);

        return InterviewRadarResponse.builder()
                .latestRadar(latestRadar)
                .dimensionTrends(dimensionTrends)
                .blindSpotTips(blindSpotTips)
                .sessionCount(sessions.size())
                .build();
    }

    /**
     * 构建最新一次面试的雷达数据。
     */
    private RadarDataVO buildLatestRadar(InterviewSession latestSession,
                                          Map<String, List<InterviewDimensionScore>> scoresBySession) {
        List<InterviewDimensionScore> scores = scoresBySession.get(latestSession.getSessionId());
        Map<String, DimensionScoreVO> dimensions = new LinkedHashMap<>();
        if (scores != null) {
            for (InterviewDimensionScore s : scores) {
                dimensions.put(s.getDimensionKey(), DimensionScoreVO.builder()
                        .score(s.getScore())
                        .comment(s.getComment())
                        .strengths(parseJsonStringList(s.getStrengths()))
                        .weaknesses(parseJsonStringList(s.getWeaknesses()))
                        .build());
            }
        }
        String createTime = latestSession.getCreateTime() != null
                ? latestSession.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
        return RadarDataVO.builder()
                .dimensions(dimensions)
                .sessionId(latestSession.getSessionId())
                .createTime(createTime)
                .build();
    }

    /**
     * 构建各维度趋势数据：每个维度一条折线，包含多次面试的得分变化。
     */
    private List<DimensionTrendVO> buildDimensionTrends(List<InterviewSession> scoredSessions,
                                                         Map<String, List<InterviewDimensionScore>> scoresBySession) {
        // 按时间正序排列
        List<InterviewSession> chronological = new ArrayList<>(scoredSessions);
        Collections.reverse(chronological);

        List<DimensionTrendVO> trends = new ArrayList<>();
        for (String dimKey : DIMENSION_KEYS) {
            List<ScorePoint> points = new ArrayList<>();
            for (InterviewSession session : chronological) {
                List<InterviewDimensionScore> scores = scoresBySession.get(session.getSessionId());
                if (scores == null) continue;
                scores.stream()
                        .filter(s -> dimKey.equals(s.getDimensionKey()))
                        .findFirst()
                        .ifPresent(s -> {
                            String date = session.getCreateTime() != null
                                    ? session.getCreateTime().format(DATE_FMT) : "";
                            points.add(ScorePoint.builder().date(date).score(s.getScore()).build());
                        });
            }
            trends.add(DimensionTrendVO.builder()
                    .dimensionKey(dimKey)
                    .dimensionLabel(DIMENSION_LABELS.getOrDefault(dimKey, dimKey))
                    .points(points)
                    .build());
        }
        return trends;
    }

    /**
     * 盲区分析：标记持续低分或下降趋势的维度。
     * 至少 2 次面试才能判断趋势，最多取最近 BLIND_SPOT_SESSION_COUNT 次计算均分。
     */
    private List<BlindSpotTipVO> analyzeBlindSpots(List<InterviewSession> scoredSessions,
                                                     Map<String, List<InterviewDimensionScore>> scoresBySession) {
        // 取最近 N 次（scoredSessions 已按时间倒序）
        List<InterviewSession> recentSessions = scoredSessions.stream()
                .limit(BLIND_SPOT_SESSION_COUNT)
                .collect(Collectors.toList());

        if (recentSessions.size() < 2) {
            return Collections.emptyList();
        }

        List<BlindSpotTipVO> tips = new ArrayList<>();

        for (String dimKey : DIMENSION_KEYS) {
            // 收集该维度在最近 N 次面试中的分数（按时间倒序：index 0 = 最新）
            List<Integer> scores = new ArrayList<>();
            for (InterviewSession session : recentSessions) {
                List<InterviewDimensionScore> sessionScores = scoresBySession.get(session.getSessionId());
                if (sessionScores == null) continue;
                sessionScores.stream()
                        .filter(s -> dimKey.equals(s.getDimensionKey()))
                        .findFirst()
                        .ifPresent(s -> scores.add(s.getScore()));
            }
            if (scores.size() < 2) continue;

            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            String label = DIMENSION_LABELS.getOrDefault(dimKey, dimKey);

            // 规则1：持续低分（均分 < 60）
            if (avg < PERSISTENT_LOW_THRESHOLD) {
                tips.add(BlindSpotTipVO.builder()
                        .dimensionKey(dimKey)
                        .dimensionLabel(label)
                        .type("persistent_low")
                        .tip("「" + label + "」近 " + scores.size() + " 次面试平均分仅 " + Math.round(avg) + " 分，属于持续薄弱项")
                        .suggestions(DIMENSION_SUGGESTIONS.getOrDefault(dimKey, Collections.emptyList()))
                        .averageScore(avg)
                        .build());
                continue;
            }

            // 规则2：下降趋势（最新 < 上一次 > 5 分 且 最新 < 70）
            int latestScore = scores.get(0);
            int previousScore = scores.get(1);
            if (previousScore - latestScore > DECLINING_DROP_THRESHOLD && latestScore < DECLINING_SCORE_THRESHOLD) {
                tips.add(BlindSpotTipVO.builder()
                        .dimensionKey(dimKey)
                        .dimensionLabel(label)
                        .type("declining_trend")
                        .tip("「" + label + "」最近一次得分 " + latestScore + " 分，较上次下降 " + (previousScore - latestScore) + " 分，呈下滑趋势")
                        .suggestions(DIMENSION_SUGGESTIONS.getOrDefault(dimKey, Collections.emptyList()))
                        .averageScore(avg)
                        .build());
            }
        }
        return tips;
    }

    /**
     * 解析 JSON 字符串数组（如 strengths/weaknesses 字段）。
     */
    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("[成长中心] 解析JSON字符串数组失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
