package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.resume.ResumeDocumentUpdateRequest;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;
import com.airesume.server.dto.resume.ResumePolishAnalyzeRequest;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.entity.ResumePolishRecord;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.airesume.server.util.TextNormalizeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * AI 简历润色服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumePolishServiceImpl extends ServiceImpl<ResumePolishRecordMapper, ResumePolishRecord>
        implements ResumePolishService {

    private static final String SOURCE_TYPE_RESUME_ONLY = "仅基于简历";
    private static final String SOURCE_TYPE_RESUME_WITH_JD = "基于简历+JD";

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumeAiService resumeAiService;
    private final ResumeContentExtractor resumeContentExtractor;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    /** 注入代理对象，确保 @Transactional 自调用生效 */
    @Lazy
    @Autowired
    private ResumePolishServiceImpl self;

    @Override
    public ResumePolishAnalyzeResponse analyzeResumePolish(Long userId, ResumePolishAnalyzeRequest request) {
        // 阶段1：加载任务、校验输入（非事务）
        Long resumeTaskId = parseResumeTaskId(request.getResumeTaskId());
        ResumeDiagnosisTask task = loadOwnedTask(userId, resumeTaskId);

        String resumeText = resolveResumeText(task, request.getResumeText());
        if (resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }

        ResumeJobMatchRecord latestJobMatchRecord = findLatestJobMatchRecord(userId, resumeTaskId);
        String jdText = TextNormalizeUtil.normalizeText(request.getJdText());
        if (jdText.isBlank() && latestJobMatchRecord != null) {
            jdText = TextNormalizeUtil.normalizeText(latestJobMatchRecord.getJdText());
        }

        ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis =
                resumeJobMatchService.getLatestAnalysis(userId, resumeTaskId);
        String sourceType = jdText.isBlank() ? SOURCE_TYPE_RESUME_ONLY : SOURCE_TYPE_RESUME_WITH_JD;

        // 阶段2：AI 调用（非事务）— 不持有数据库连接
        ResumePolishAiResult aiResult = resumeAiService.polishResume(
                resumeText, jdText, latestJobMatchAnalysis, userId, Boolean.TRUE.equals(request.getFallbackToPlatform()));

        // 阶段3：保存结果（事务内）
        return self.savePolishResult(userId, resumeTaskId, resumeText, jdText, sourceType, aiResult);
    }

    /**
     * 事务内操作：保存润色结果并发送通知
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumePolishAnalyzeResponse savePolishResult(Long userId, Long resumeTaskId, String resumeText,
                                                         String jdText, String sourceType, ResumePolishAiResult aiResult) {
        ResumePolishRecord record = new ResumePolishRecord();
        record.setUserId(userId);
        record.setResumeTaskId(resumeTaskId);
        record.setSourceResumeText(resumeText);
        record.setJdText(jdText.isBlank() ? null : jdText);
        record.setPolishedResumeText(aiResult.getPolishedResumeText());
        record.setModificationNotes(toJson(aiResult.getModificationNotes()));
        record.setSourceType(sourceType);
        save(record);

        notificationService.createNotification(
                userId,
                "polish",
                "AI 简历润色完成",
                "你的简历润色结果已生成，点击即可查看优化内容。",
                "resume_polish",
                String.valueOf(resumeTaskId));

        return ResumePolishAnalyzeResponse.builder()
                .polishRecordId(String.valueOf(record.getId()))
                .resumeTaskId(String.valueOf(resumeTaskId))
                .polishedResumeText(aiResult.getPolishedResumeText())
                .modificationNotes(aiResult.getModificationNotes())
                .sourceType(sourceType)
                .createTime(record.getCreateTime())
                .build();
    }

    @Override
    public ResumePolishAnalyzeResponse getLatestPolishResult(Long userId, Long resumeTaskId) {
        LambdaQueryWrapper<ResumePolishRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumePolishRecord::getUserId, userId)
                .eq(ResumePolishRecord::getResumeTaskId, resumeTaskId)
                // 结果页需要润色全文和结构化文档，显式补回默认不查询的大字段。
                .select(ResumePolishRecord::getId, ResumePolishRecord::getUserId,
                        ResumePolishRecord::getResumeTaskId, ResumePolishRecord::getPolishedResumeText,
                        ResumePolishRecord::getDocumentJson, ResumePolishRecord::getEditedPlainText,
                        ResumePolishRecord::getModificationNotes, ResumePolishRecord::getSourceType,
                        ResumePolishRecord::getCreateTime)
                .orderByDesc(ResumePolishRecord::getCreateTime)
                .last("limit 1");

        ResumePolishRecord record = getOne(wrapper, false);
        if (record == null) {
            return null;
        }

        return ResumePolishAnalyzeResponse.builder()
                .polishRecordId(String.valueOf(record.getId()))
                .resumeTaskId(String.valueOf(record.getResumeTaskId()))
                .polishedResumeText(record.getPolishedResumeText())
                .documentJson(record.getDocumentJson())
                .editedPlainText(record.getEditedPlainText())
                .modificationNotes(parseNotes(record.getModificationNotes()))
                .sourceType(record.getSourceType())
                .createTime(record.getCreateTime())
                .build();
    }

    @Override
    public void updateDocument(Long userId, Long polishRecordId, ResumeDocumentUpdateRequest request) {
        ResumePolishRecord record = getOne(new LambdaQueryWrapper<ResumePolishRecord>()
                // 保存文档只校验归属，不加载润色全文等大字段。
                .select(ResumePolishRecord::getId, ResumePolishRecord::getUserId)
                .eq(ResumePolishRecord::getId, polishRecordId)
                .last("limit 1"), false);
        if (record == null) {
            throw new BusinessException("润色记录不存在");
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new BusinessException("无权修改该润色记录");
        }

        LambdaUpdateWrapper<ResumePolishRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumePolishRecord::getId, polishRecordId)
                .set(ResumePolishRecord::getDocumentJson, request.getDocumentJson())
                .set(ResumePolishRecord::getEditedPlainText, request.getEditedPlainText());
        boolean updated = update(wrapper);
        if (!updated) {
            throw new BusinessException("保存失败，润色记录可能已被删除");
        }
        log.info("保存简历编辑文档，userId: {}, polishRecordId: {}", userId, polishRecordId);
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

    /**
     * 读取最近一次 JD 匹配记录，用于回退提取 JD 原文快照。
     */
    private ResumeJobMatchRecord findLatestJobMatchRecord(Long userId, Long resumeTaskId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                .eq(ResumeJobMatchRecord::getResumeTaskId, resumeTaskId)
                // 润色缺少 JD 入参时需要复用最近 JD 文本快照。
                .select(ResumeJobMatchRecord::getId, ResumeJobMatchRecord::getUserId,
                        ResumeJobMatchRecord::getResumeTaskId, ResumeJobMatchRecord::getResumeText,
                        ResumeJobMatchRecord::getJdText, ResumeJobMatchRecord::getAnalysisResult,
                        ResumeJobMatchRecord::getMatchScore, ResumeJobMatchRecord::getCreateTime)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return resumeJobMatchRecordMapper.selectOne(wrapper);
    }

    private ResumeDiagnosisTask loadOwnedTask(Long userId, Long resumeTaskId) {
        ResumeDiagnosisTask task = resumeDiagnosisTaskMapper.selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                // 润色链路需要任务缓存文本，避免大字段默认不加载导致重复解析。
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

    /**
     * 润色说明以 JSON 数组落库，便于结果页直接回显。
     */
    private String toJson(List<String> notes) {
        try {
            return objectMapper.writeValueAsString(notes);
        } catch (JsonProcessingException e) {
            throw new BusinessException("润色结果保存失败");
        }
    }

    private List<String> parseNotes(String modificationNotes) {
        if (modificationNotes == null || modificationNotes.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(modificationNotes, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("解析润色说明失败，recordNotes: {}", modificationNotes, e);
            return List.of(modificationNotes);
        }
    }
    @Override
    public ResumePolishAnalyzeResponse analyzeResumePolish(
            Long userId,
            ResumePolishAnalyzeRequest request,
            Consumer<ResumePolishProgressEvent> progressConsumer) {
        emitProgress(progressConsumer, "preparing", "preparing", "正在准备简历与岗位信息", 20, null);

        Long resumeTaskId = parseResumeTaskId(request.getResumeTaskId());
        ResumeDiagnosisTask task = loadOwnedTask(userId, resumeTaskId);

        String resumeText = resolveResumeText(task, request.getResumeText());
        if (resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }

        ResumeJobMatchRecord latestJobMatchRecord = findLatestJobMatchRecord(userId, resumeTaskId);
        String jdText = TextNormalizeUtil.normalizeText(request.getJdText());
        if (jdText.isBlank() && latestJobMatchRecord != null) {
            jdText = TextNormalizeUtil.normalizeText(latestJobMatchRecord.getJdText());
        }

        ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis =
                resumeJobMatchService.getLatestAnalysis(userId, resumeTaskId);
        String sourceType = jdText.isBlank() ? SOURCE_TYPE_RESUME_ONLY : SOURCE_TYPE_RESUME_WITH_JD;

        emitProgress(progressConsumer, "calling_ai", "calling_ai", "AI 正在生成润色结果", 60, null);
        ResumePolishAiResult aiResult = resumeAiService.polishResume(
                resumeText, jdText, latestJobMatchAnalysis, userId, Boolean.TRUE.equals(request.getFallbackToPlatform()));

        emitProgress(progressConsumer, "saving", "saving", "正在保存润色结果", 90, null);
        return self.savePolishResult(userId, resumeTaskId, resumeText, jdText, sourceType, aiResult);
    }

    private void emitProgress(
            Consumer<ResumePolishProgressEvent> progressConsumer,
            String eventName,
            String stage,
            String message,
            int progress,
            Object data) {
        if (progressConsumer == null) {
            return;
        }
        progressConsumer.accept(new ResumePolishProgressEvent(eventName, stage, message, progress, data));
    }
}
