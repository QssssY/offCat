package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewJobTargetedFeedback;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.MockInterviewJobTargetService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.airesume.server.util.TextNormalizeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 岗位定向模拟面试服务实现。
 * 该实现负责把 JD、简历文本和最近一次 JD 对比结果统一整理成面试上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockInterviewJobTargetServiceImpl
        extends ServiceImpl<MockInterviewJobTargetRecordMapper, MockInterviewJobTargetRecord>
        implements MockInterviewJobTargetService {

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumeContentExtractor resumeContentExtractor;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewJobTargetContext resolveContext(Long userId, CreateSessionRequest request) {
        InterviewJobTargetContext fallbackContext = InterviewJobTargetContext.builder().jobTargeted(false).build();
        Long resumeTaskId = parseOptionalLong(request.getResumeTaskId(), "简历任务 ID");
        if (!Boolean.TRUE.equals(request.getJobTargeted())) {
            InterviewJobTargetContext generalContext = buildGeneralResumeContext(userId, resumeTaskId);
            log.info("普通模拟面试上下文解析完成, userId: {}, hasResume: {}, resumeTaskId: {}",
                    userId,
                    generalContext != null && generalContext.getResumeText() != null && !generalContext.getResumeText().isBlank(),
                    generalContext == null ? null : generalContext.getResumeTaskId());
            return generalContext == null ? fallbackContext : generalContext;
        }

        Long jobMatchRecordId = parseOptionalLong(request.getJobMatchRecordId(), "岗位对比记录 ID");
        String jdText = TextNormalizeUtil.normalizeText(request.getJdText());

        ResumeJobMatchRecord selectedRecord = resolveJobMatchRecord(
                userId,
                resumeTaskId,
                jobMatchRecordId,
                Boolean.TRUE.equals(request.getUseLatestJobMatch()),
                jdText
        );

        if (jdText.isBlank() && selectedRecord != null) {
            jdText = TextNormalizeUtil.normalizeText(selectedRecord.getJdText());
        }

        // 没有可用 JD 时，自动回落到普通模拟面试，不强制报错。
        if (jdText.isBlank()) {
            InterviewJobTargetContext generalContext = buildGeneralResumeContext(
                    userId,
                    resolveResumeTaskIdValue(resumeTaskId, selectedRecord)
            );
            log.info("岗位定向缺少 JD，自动回退普通模拟面试, userId: {}, resumeTaskId: {}",
                    userId,
                    generalContext == null ? null : generalContext.getResumeTaskId());
            return generalContext == null ? fallbackContext : generalContext;
        }

        Long resolvedResumeTaskId = resolveResumeTaskIdValue(resumeTaskId, selectedRecord);
        String resumeText = loadResumeText(userId, resolvedResumeTaskId);
        if (resumeText.isBlank() && selectedRecord != null) {
            resumeText = TextNormalizeUtil.normalizeText(selectedRecord.getResumeText());
        }
        if (resumeText.isBlank()) {
            InterviewJobTargetContext latestResumeContext = buildGeneralResumeContext(userId, null);
            if (latestResumeContext != null) {
                resolvedResumeTaskId = parseOptionalLong(latestResumeContext.getResumeTaskId(), "简历任务 ID");
                resumeText = TextNormalizeUtil.normalizeText(latestResumeContext.getResumeText());
            }
        }

        InterviewJobTargetContext context = InterviewJobTargetContext.builder()
                .jobTargeted(true)
                .sourceType(resolveSourceType(jdText, request, selectedRecord))
                .resumeTaskId(resolvedResumeTaskId == null ? null : String.valueOf(resolvedResumeTaskId))
                .jdText(jdText)
                .jobMatchRecordId(selectedRecord == null ? null : String.valueOf(selectedRecord.getId()))
                .resumeText(resumeText)
                .matchedKeywords(new ArrayList<>())
                .missingKeywords(new ArrayList<>())
                .suggestions(new ArrayList<>())
                .build();

        if (selectedRecord != null) {
            ResumeJobMatchAnalyzeResponse analysis = parseAnalysisResponse(selectedRecord);
            if (analysis != null) {
                context.setMatchedKeywords(defaultList(analysis.getMatchedKeywords()));
                context.setMissingKeywords(defaultList(analysis.getMissingKeywords()));
                context.setSuggestions(defaultList(analysis.getSuggestions()));
            }
        }
        log.info("岗位定向模拟上下文解析完成, userId: {}, resumeTaskId: {}, jobMatchRecordId: {}, hasResume: {}, hasJd: {}",
                userId,
                context.getResumeTaskId(),
                context.getJobMatchRecordId(),
                !context.getResumeText().isBlank(),
                !context.getJdText().isBlank());
        return context;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "interview:jobTarget", key = "#userId + '::' + #sessionId")
    public void saveSessionContext(Long userId, String sessionId, InterviewJobTargetContext context, String openingQuestion) {
        if (context == null || !Boolean.TRUE.equals(context.getJobTargeted())) {
            return;
        }

        MockInterviewJobTargetRecord record = new MockInterviewJobTargetRecord();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setResumeTaskId(parseOptionalLong(context.getResumeTaskId(), "简历任务 ID"));
        record.setJdText(context.getJdText());
        record.setJobMatchRecordId(parseOptionalLong(context.getJobMatchRecordId(), "岗位对比记录 ID"));
        record.setGeneratedQuestions(openingQuestion);
        record.setSourceType(context.getSourceType());
        save(record);
    }

    @Override
    @Cacheable(value = "interview:jobTarget", key = "#userId + '::' + #sessionId")
    public InterviewJobTargetContext getSessionContext(Long userId, String sessionId) {
        LambdaQueryWrapper<MockInterviewJobTargetRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MockInterviewJobTargetRecord::getUserId, userId)
                .eq(MockInterviewJobTargetRecord::getSessionId, sessionId)
                // 会话详情需要 JD 快照和岗位定向反馈，显式补回大字段。
                .select(MockInterviewJobTargetRecord::getId, MockInterviewJobTargetRecord::getUserId,
                        MockInterviewJobTargetRecord::getSessionId, MockInterviewJobTargetRecord::getResumeTaskId,
                        MockInterviewJobTargetRecord::getJdText, MockInterviewJobTargetRecord::getJobMatchRecordId,
                        MockInterviewJobTargetRecord::getGeneratedQuestions,
                        MockInterviewJobTargetRecord::getJobTargetedFeedback,
                        MockInterviewJobTargetRecord::getSourceType, MockInterviewJobTargetRecord::getCreateTime)
                .last("limit 1");
        MockInterviewJobTargetRecord record = getOne(wrapper, false);
        if (record == null) {
            // 普通模拟面试没有岗位定向记录，返回可缓存的空上下文，避免轮询详情时反复穿透查询数据库。
            return InterviewJobTargetContext.builder()
                    .jobTargeted(false)
                    .sourceType("none")
                    .build();
        }

        InterviewJobTargetContext context = InterviewJobTargetContext.builder()
                .jobTargeted(true)
                .sourceType(record.getSourceType())
                .resumeTaskId(record.getResumeTaskId() == null ? null : String.valueOf(record.getResumeTaskId()))
                .jdText(record.getJdText())
                .jobMatchRecordId(record.getJobMatchRecordId() == null ? null : String.valueOf(record.getJobMatchRecordId()))
                .matchedKeywords(new ArrayList<>())
                .missingKeywords(new ArrayList<>())
                .suggestions(new ArrayList<>())
                .jobTargetedFeedback(parseFeedback(record.getJobTargetedFeedback()))
                .build();

        ResumeJobMatchRecord jobMatchRecord = resumeJobMatchService.getOwnedRecordById(userId, record.getJobMatchRecordId());
        if (jobMatchRecord != null) {
            ResumeJobMatchAnalyzeResponse analysis = parseAnalysisResponse(jobMatchRecord);
            if (analysis != null) {
                context.setMatchedKeywords(defaultList(analysis.getMatchedKeywords()));
                context.setMissingKeywords(defaultList(analysis.getMissingKeywords()));
                context.setSuggestions(defaultList(analysis.getSuggestions()));
            }
        }
        String resumeText = loadResumeText(userId, record.getResumeTaskId());
        if (resumeText.isBlank() && jobMatchRecord != null) {
            resumeText = TextNormalizeUtil.normalizeText(jobMatchRecord.getResumeText());
        }
        context.setResumeText(resumeText);

        return context;
    }

    @Override
    public Map<String, InterviewJobTargetContext> getSessionContextSummaryMap(Long userId, Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<MockInterviewJobTargetRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MockInterviewJobTargetRecord::getUserId, userId)
                .in(MockInterviewJobTargetRecord::getSessionId, sessionIds)
                // 历史列表只读轻字段，不加载 JD、问题快照和反馈正文。
                .select(MockInterviewJobTargetRecord::getId, MockInterviewJobTargetRecord::getUserId,
                        MockInterviewJobTargetRecord::getSessionId, MockInterviewJobTargetRecord::getSourceType,
                        MockInterviewJobTargetRecord::getCreateTime)
                .orderByDesc(MockInterviewJobTargetRecord::getCreateTime)
                .orderByDesc(MockInterviewJobTargetRecord::getId);

        List<MockInterviewJobTargetRecord> records = list(wrapper);
        Map<String, InterviewJobTargetContext> contextMap = new LinkedHashMap<>();
        for (MockInterviewJobTargetRecord record : records) {
            // 历史列表只需要判断是否为岗位定向以及来源类型，不需要加载完整上下文。
            contextMap.putIfAbsent(record.getSessionId(), InterviewJobTargetContext.builder()
                    .jobTargeted(true)
                    .sourceType(record.getSourceType())
                    .build());
        }
        return contextMap;
    }

    @Override
    public InterviewJobTargetContext resolveLatestResumeContext(Long userId) {
        return buildGeneralResumeContext(userId, null);
    }

    @Override
    public InterviewJobTargetedFeedback buildFeedback(InterviewEvaluationReport report, InterviewJobTargetContext context) {
        if (report == null || context == null || !Boolean.TRUE.equals(context.getJobTargeted())) {
            return null;
        }

        String performance = "";
        if (report.getJobMatch() != null && report.getJobMatch().getComment() != null) {
            performance = report.getJobMatch().getComment();
        } else if (report.getSummary() != null) {
            performance = report.getSummary();
        }

        return InterviewJobTargetedFeedback.builder()
                .jobMatchPerformance(performance)
                .strengths(defaultList(report.getStrengths()))
                .weaknesses(mergeWeaknesses(report))
                .improvementSuggestions(defaultList(report.getImprovementSuggestions()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeedback(String sessionId, InterviewJobTargetedFeedback feedback) {
        if (sessionId == null || sessionId.isBlank() || feedback == null) {
            return;
        }
        LambdaQueryWrapper<MockInterviewJobTargetRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MockInterviewJobTargetRecord::getSessionId, sessionId)
                // 只更新反馈字段，查询轻字段即可完成存在性判断。
                .select(MockInterviewJobTargetRecord::getId, MockInterviewJobTargetRecord::getSessionId)
                .last("limit 1");
        MockInterviewJobTargetRecord record = getOne(wrapper, false);
        if (record == null) {
            return;
        }
        record.setJobTargetedFeedback(writeJson(feedback));
        updateById(record);
    }

    /**
     * 逻辑删除当前用户的岗位定向上下文。
     */
    @Override
    public int logicalDeleteByUserId(Long userId) {
        return getBaseMapper().logicalDeleteByUserId(userId);
    }

    /**
     * 按会话批量逻辑删除岗位定向上下文。
     */
    @Override
    public int logicalDeleteBySessionIds(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }
        return getBaseMapper().logicalDeleteBySessionIds(sessionIds);
    }

    /**
     * 解析应当复用的岗位 JD 对比记录。
     * 优先级为：指定记录 ID > 最近一次匹配记录 > 与手动 JD 相同的最近记录。
     */
    private ResumeJobMatchRecord resolveJobMatchRecord(
            Long userId,
            Long resumeTaskId,
            Long jobMatchRecordId,
            boolean useLatestJobMatch,
            String jdText
    ) {
        if (jobMatchRecordId != null) {
            return resumeJobMatchService.getOwnedRecordById(userId, jobMatchRecordId);
        }

        ResumeJobMatchRecord latestRecord = resumeTaskId == null
                ? resumeJobMatchService.getLatestRecord(userId)
                : resumeJobMatchService.getLatestRecord(userId, resumeTaskId);

        if (latestRecord == null) {
            return null;
        }
        if (useLatestJobMatch) {
            return latestRecord;
        }
        if (!jdText.isBlank() && Objects.equals(TextNormalizeUtil.normalizeText(latestRecord.getJdText()), jdText)) {
            return latestRecord;
        }
        if (jdText.isBlank()) {
            return latestRecord;
        }
        return null;
    }

    /**
     * 从简历任务中兜底提取简历文本。
     */
    private String loadResumeText(Long userId, Long resumeTaskId) {
        if (resumeTaskId == null) {
            return "";
        }
        ResumeDiagnosisTask task = resumeDiagnosisTaskMapper.selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                // 面试上下文需要简历文本，显式补回 resume_text。
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId,
                        ResumeDiagnosisTask::getFileUrl, ResumeDiagnosisTask::getResumeText,
                        ResumeDiagnosisTask::getParseMode, ResumeDiagnosisTask::getParseMessage,
                        ResumeDiagnosisTask::getIsDeleted)
                .eq(ResumeDiagnosisTask::getId, resumeTaskId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException("简历诊断任务不存在");
        }
        if (!Objects.equals(task.getUserId(), userId)) {
            throw new BusinessException("无权访问该简历诊断任务");
        }
        if (task.getResumeText() != null && !task.getResumeText().isBlank()) {
            return TextNormalizeUtil.normalizeText(task.getResumeText());
        }
        if (task.getFileUrl() == null || task.getFileUrl().isBlank()) {
            return "";
        }
        try {
            ResumeParseResult parseResult = resumeContentExtractor.extract(task.getFileUrl());
            task.setResumeText(parseResult.getText());
            task.setParseMode(parseResult.getParseMode());
            task.setParseMessage(parseResult.getParseMessage());
            resumeDiagnosisTaskMapper.updateById(task);
            return TextNormalizeUtil.normalizeText(parseResult.getText());
        } catch (Exception e) {
            log.warn("提取简历文本失败, userId: {}, resumeTaskId: {}", userId, resumeTaskId, e);
            return "";
        }
    }

    /**
     * 合并短板与缺失能力，生成更适合前端展示的不足列表。
     */
    private List<String> mergeWeaknesses(InterviewEvaluationReport report) {
        List<String> merged = new ArrayList<>();
        merged.addAll(defaultList(report.getWeaknesses()));
        merged.addAll(defaultList(report.getMissingCompetencies()));
        return merged.stream().distinct().toList();
    }

    private String resolveSourceType(String jdText, CreateSessionRequest request, ResumeJobMatchRecord selectedRecord) {
        boolean hasManualJd = jdText != null && !jdText.isBlank();
        boolean hasJobMatch = selectedRecord != null;
        if (hasManualJd && hasJobMatch) {
            return "manual_jd_with_job_match";
        }
        if (hasManualJd) {
            return "manual_jd";
        }
        if (hasJobMatch) {
            return "latest_job_match";
        }
        return "general";
    }

    private String resolveResumeTaskId(Long resumeTaskId, ResumeJobMatchRecord selectedRecord) {
        if (resumeTaskId != null) {
            return String.valueOf(resumeTaskId);
        }
        if (selectedRecord != null && selectedRecord.getResumeTaskId() != null) {
            return String.valueOf(selectedRecord.getResumeTaskId());
        }
        return null;
    }

    /**
     * 统一解析本轮应复用的简历任务 ID。
     * 优先级：显式传入 > 岗位对比记录中的简历任务。
     */
    private Long resolveResumeTaskIdValue(Long resumeTaskId, ResumeJobMatchRecord selectedRecord) {
        if (resumeTaskId != null) {
            return resumeTaskId;
        }
        if (selectedRecord != null) {
            return selectedRecord.getResumeTaskId();
        }
        return null;
    }

    /**
     * 为普通模拟面试构造简历上下文。
     * 如果用户显式传入了简历任务，则优先使用该任务；否则回退到最近一次已完成简历诊断。
     */
    private InterviewJobTargetContext buildGeneralResumeContext(Long userId, Long preferredResumeTaskId) {
        Long resolvedResumeTaskId = preferredResumeTaskId;
        String resumeText = loadResumeText(userId, preferredResumeTaskId);

        // 【兜底说明】
        // 普通模拟面试也允许面试官“看过候选人简历”。
        // 如果本次请求没有显式传入 resumeTaskId，就回退到最近一次已完成的简历诊断任务。
        if (resumeText.isBlank()) {
            ResumeDiagnosisTask latestResumeTask = findLatestCompletedResumeTask(userId);
            if (latestResumeTask != null) {
                resolvedResumeTaskId = latestResumeTask.getId();
                resumeText = loadResumeText(userId, latestResumeTask.getId());
            }
        }
        if (resumeText.isBlank() || resolvedResumeTaskId == null) {
            return null;
        }
        return InterviewJobTargetContext.builder()
                .jobTargeted(false)
                .sourceType(preferredResumeTaskId != null ? "specified_resume" : "latest_resume")
                .resumeTaskId(String.valueOf(resolvedResumeTaskId))
                .resumeText(resumeText)
                .matchedKeywords(new ArrayList<>())
                .missingKeywords(new ArrayList<>())
                .suggestions(new ArrayList<>())
                .build();
    }

    /**
     * 查询用户最近一次已完成的简历诊断任务。
     * 仅在普通模拟面试缺少显式简历上下文时作为兜底，不改变已存在的岗位定向链路。
     */
    private ResumeDiagnosisTask findLatestCompletedResumeTask(Long userId) {
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getUserId, userId)
                .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_COMPLETED)
                // 这里只取最近任务 ID，后续 loadResumeText 再显式读取简历文本。
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId,
                        ResumeDiagnosisTask::getStatus, ResumeDiagnosisTask::getUpdateTime,
                        ResumeDiagnosisTask::getCreateTime)
                .orderByDesc(ResumeDiagnosisTask::getUpdateTime)
                .orderByDesc(ResumeDiagnosisTask::getCreateTime)
                .last("limit 1");
        return resumeDiagnosisTaskMapper.selectOne(wrapper);
    }

    private ResumeJobMatchAnalyzeResponse parseAnalysisResponse(ResumeJobMatchRecord record) {
        if (record == null || record.getAnalysisResult() == null || record.getAnalysisResult().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getAnalysisResult(), ResumeJobMatchAnalyzeResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("解析岗位 JD 对比分析记录失败, recordId: {}", record.getId(), e);
            return null;
        }
    }

    private InterviewJobTargetedFeedback parseFeedback(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawJson, InterviewJobTargetedFeedback.class);
        } catch (JsonProcessingException e) {
            log.warn("解析岗位定向反馈失败", e);
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("岗位定向数据保存失败");
        }
    }

    private List<String> defaultList(List<String> value) {
        return value == null ? new ArrayList<>() : new ArrayList<>(value);
    }

    private Long parseOptionalLong(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

}
