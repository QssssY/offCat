package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeRequest;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.airesume.server.util.TextNormalizeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 简历与 JD 匹配分析服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeJobMatchServiceImpl extends ServiceImpl<ResumeJobMatchRecordMapper, ResumeJobMatchRecord>
        implements ResumeJobMatchService {

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeContentExtractor resumeContentExtractor;
    private final ObjectMapper objectMapper;
    private final ResumeAiService resumeAiService;

    /** 注入代理对象，确保 @Transactional 自调用生效 */
    @Lazy
    @Autowired
    private ResumeJobMatchServiceImpl self;

    @Override
    public ResumeJobMatchAnalyzeResponse analyzeJobMatch(Long userId, ResumeJobMatchAnalyzeRequest request) {
        // 阶段1：加载任务、校验输入（非事务）
        Long resumeTaskId = parseResumeTaskId(request.getResumeTaskId());
        ResumeDiagnosisTask task = loadOwnedTask(userId, resumeTaskId);

        String jdText = TextNormalizeUtil.normalizeText(request.getJdText());
        if (jdText.isBlank()) {
            throw new BusinessException("岗位 JD 文本不能为空");
        }

        String resumeText = resolveResumeText(task, request.getResumeText());
        if (resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }

        // 阶段2：AI 调用（非事务）— 不持有数据库连接
        String aiResultJson = resumeAiService.diagnoseJobMatch(
                resumeText, jdText, userId, Boolean.TRUE.equals(request.getFallbackToPlatform()));
        ResumeJobMatchAnalyzeResponse response = parseAiResult(aiResultJson, resumeTaskId);

        // 阶段3：保存结果（事务内）
        self.saveJobMatchRecord(userId, resumeTaskId, resumeText, jdText, response);

        return response;
    }

    /**
     * 事务内操作：保存岗位匹配分析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveJobMatchRecord(Long userId, Long resumeTaskId, String resumeText, String jdText,
                                   ResumeJobMatchAnalyzeResponse response) {
        ResumeJobMatchRecord record = new ResumeJobMatchRecord();
        record.setUserId(userId);
        record.setResumeTaskId(resumeTaskId);
        record.setResumeText(resumeText);
        record.setJdText(jdText);
        record.setMatchScore(response.getMatchScore());
        record.setAnalysisResult(toAnalysisJson(response));
        save(record);

        response.setAnalysisId(String.valueOf(record.getId()));
        response.setCreateTime(record.getCreateTime());
    }

    @Override
    public ResumeJobMatchAnalyzeResponse getLatestAnalysis(Long userId, Long resumeTaskId) {
        ResumeJobMatchRecord record = getLatestRecord(userId, resumeTaskId);
        if (record == null || record.getAnalysisResult() == null || record.getAnalysisResult().isBlank()) {
            return null;
        }

        try {
            ResumeJobMatchAnalyzeResponse response =
                    objectMapper.readValue(record.getAnalysisResult(), ResumeJobMatchAnalyzeResponse.class);
            response.setAnalysisId(String.valueOf(record.getId()));
            response.setResumeTaskId(String.valueOf(record.getResumeTaskId()));
            response.setCreateTime(record.getCreateTime());
            return response;
        } catch (JsonProcessingException e) {
            log.error("解析岗位 JD 匹配记录失败, recordId: {}", record.getId(), e);
            return null;
        }
    }

    @Override
    public ResumeJobMatchRecord getLatestRecord(Long userId, Long resumeTaskId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                .eq(ResumeJobMatchRecord::getResumeTaskId, resumeTaskId)
                // 最新记录可能被润色/岗位定向链路复用，需要显式补回文本快照。
                .select(ResumeJobMatchRecord::getId, ResumeJobMatchRecord::getUserId,
                        ResumeJobMatchRecord::getResumeTaskId, ResumeJobMatchRecord::getResumeText,
                        ResumeJobMatchRecord::getJdText, ResumeJobMatchRecord::getMatchScore,
                        ResumeJobMatchRecord::getAnalysisResult, ResumeJobMatchRecord::getCreateTime)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return getOne(wrapper, false);
    }

    @Override
    public ResumeJobMatchRecord getLatestRecord(Long userId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                // 岗位定向/润色复用最近记录时需要 JD、简历快照和分析 JSON。
                .select(ResumeJobMatchRecord::getId, ResumeJobMatchRecord::getUserId,
                        ResumeJobMatchRecord::getResumeTaskId, ResumeJobMatchRecord::getResumeText,
                        ResumeJobMatchRecord::getJdText, ResumeJobMatchRecord::getMatchScore,
                        ResumeJobMatchRecord::getAnalysisResult, ResumeJobMatchRecord::getCreateTime)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return getOne(wrapper, false);
    }

    @Override
    public ResumeJobMatchRecord getOwnedRecordById(Long userId, Long recordId) {
        if (recordId == null) {
            return null;
        }
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getId, recordId)
                .eq(ResumeJobMatchRecord::getUserId, userId)
                // 指定记录用于岗位定向上下文，需要完整文本快照和分析结果。
                .select(ResumeJobMatchRecord::getId, ResumeJobMatchRecord::getUserId,
                        ResumeJobMatchRecord::getResumeTaskId, ResumeJobMatchRecord::getResumeText,
                        ResumeJobMatchRecord::getJdText, ResumeJobMatchRecord::getMatchScore,
                        ResumeJobMatchRecord::getAnalysisResult, ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return getOne(wrapper, false);
    }

    /**
     * 文本优先级：前端显式传入 > 任务缓存文本 > 统一解析链路。
     */
    private String resolveResumeText(ResumeDiagnosisTask task, String requestResumeText) {
        String resumeText = TextNormalizeUtil.normalizeText(requestResumeText);
        if (!resumeText.isBlank()) {
            return resumeText;
        }

        resumeText = TextNormalizeUtil.normalizeText(task.getResumeText());
        if (!resumeText.isBlank()) {
            return resumeText;
        }

        return TextNormalizeUtil.normalizeText(parseAndCacheResume(task).getText());
    }

    /**
     * 当任务缺少缓存文本时，统一走新的混合解析链路并回写任务缓存。
     */
    private ResumeParseResult parseAndCacheResume(ResumeDiagnosisTask task) {
        if (task == null || task.getFileUrl() == null || task.getFileUrl().isBlank()) {
            throw new BusinessException("简历文件不存在，无法解析");
        }

        ResumeParseResult parseResult = resumeContentExtractor.extract(task.getFileUrl());
        task.setResumeText(parseResult.getText());
        task.setParseMode(parseResult.getParseMode());
        task.setParseMessage(parseResult.getParseMessage());
        resumeDiagnosisTaskMapper.updateById(task);
        return parseResult;
    }

    private ResumeJobMatchAnalyzeResponse parseAiResult(String aiResultJson, Long resumeTaskId) {
        try {
            JsonNode root = objectMapper.readTree(aiResultJson);

            int matchScore = root.has("matchScore") ? root.get("matchScore").asInt(0) : 0;
            matchScore = Math.max(0, Math.min(matchScore, 100));

            List<String> matchedKeywords = readStringList(root, "matchedKeywords");
            List<String> missingKeywords = readStringList(root, "missingKeywords");
            List<String> suggestions = readStringList(root, "suggestions");
            String analysisSummary = root.has("analysisSummary") ? root.get("analysisSummary").asText("") : "";

            if (suggestions.isEmpty()) {
                suggestions.add("建议根据岗位 JD 优化简历中的技能与项目表述");
            }

            return ResumeJobMatchAnalyzeResponse.builder()
                    .resumeTaskId(String.valueOf(resumeTaskId))
                    .matchScore(matchScore)
                    .matchedKeywords(matchedKeywords)
                    .missingKeywords(missingKeywords)
                    .suggestions(suggestions)
                    .analysisSummary(analysisSummary)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("AI JD 匹配结果 JSON 解析失败, rawPreview: {}", buildRawPreview(aiResultJson), e);
            throw new BusinessException("AI 分析结果解析失败，请重试");
        }
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String text = item.asText("").trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private ResumeDiagnosisTask loadOwnedTask(Long userId, Long resumeTaskId) {
        ResumeDiagnosisTask task = resumeDiagnosisTaskMapper.selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                // JD 匹配需要读取任务缓存简历文本，避免 select=false 后误判为空。
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
        return task;
    }

    private Long parseResumeTaskId(String resumeTaskId) {
        if (resumeTaskId == null || resumeTaskId.isBlank()) {
            throw new BusinessException("简历诊断任务 ID 不能为空");
        }
        try {
            return Long.parseLong(resumeTaskId);
        } catch (NumberFormatException e) {
            throw new BusinessException("简历诊断任务 ID 格式不正确");
        }
    }

    private String toAnalysisJson(ResumeJobMatchAnalyzeResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException("岗位 JD 匹配结果保存失败");
        }
    }

    private String buildRawPreview(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String preview = raw.replace("\r", "\\r").replace("\n", "\\n");
        return preview.length() > 300 ? preview.substring(0, 300) + "..." : preview;
    }
}
