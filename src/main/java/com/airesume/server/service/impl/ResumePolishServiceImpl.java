package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
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
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.ResumeAiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * AI 简历润色服务实现。
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
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumePolishAnalyzeResponse analyzeResumePolish(Long userId, ResumePolishAnalyzeRequest request) {
        Long resumeTaskId = parseResumeTaskId(request.getResumeTaskId());
        ResumeDiagnosisTask task = loadOwnedTask(userId, resumeTaskId);

        // 优先使用前端传入的简历文本，缺失时再从 PDF 原文兜底提取。
        String resumeText = normalizeText(request.getResumeText());
        if (resumeText.isBlank()) {
            resumeText = normalizeText(pdfTextExtractor.extractText(task.getFileUrl()));
        }
        if (resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }

        // JD 文本优先用前端显式传入，其次回退到最近一次 JD 分析记录快照。
        ResumeJobMatchRecord latestJobMatchRecord = findLatestJobMatchRecord(userId, resumeTaskId);
        String jdText = normalizeText(request.getJdText());
        if (jdText.isBlank() && latestJobMatchRecord != null) {
            jdText = normalizeText(latestJobMatchRecord.getJdText());
        }

        ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis =
                resumeJobMatchService.getLatestAnalysis(userId, resumeTaskId);

        String sourceType = jdText.isBlank() ? SOURCE_TYPE_RESUME_ONLY : SOURCE_TYPE_RESUME_WITH_JD;

        ResumePolishAiResult aiResult = resumeAiService.polishResume(
                resumeText,
                jdText,
                latestJobMatchAnalysis
        );

        ResumePolishRecord record = new ResumePolishRecord();
        record.setUserId(userId);
        record.setResumeTaskId(resumeTaskId);
        record.setSourceResumeText(resumeText);
        record.setJdText(jdText.isBlank() ? null : jdText);
        record.setPolishedResumeText(aiResult.getPolishedResumeText());
        record.setModificationNotes(toJson(aiResult.getModificationNotes()));
        record.setSourceType(sourceType);
        save(record);

        // 创建 AI 润色完成通知
        notificationService.createNotification(
                userId, "polish", "AI 简历润色完成",
                "你的简历润色结果已生成，点击查看优化内容。",
                "resume_polish", String.valueOf(record.getId()));

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
                .modificationNotes(parseNotes(record.getModificationNotes()))
                .sourceType(record.getSourceType())
                .createTime(record.getCreateTime())
                .build();
    }

    /**
     * 读取最近一次 JD 分析记录，用于回退提取 JD 原文快照。
     */
    private ResumeJobMatchRecord findLatestJobMatchRecord(Long userId, Long resumeTaskId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                .eq(ResumeJobMatchRecord::getResumeTaskId, resumeTaskId)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return resumeJobMatchRecordMapper.selectOne(wrapper);
    }

    /**
     * 校验任务归属，防止跨用户读取简历原文。
     */
    private ResumeDiagnosisTask loadOwnedTask(Long userId, Long resumeTaskId) {
        ResumeDiagnosisTask task = resumeDiagnosisTaskMapper.selectById(resumeTaskId);
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

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ')
                .replaceAll("[\\u200B-\\u200F\\uFEFF]", "")
                .replaceAll(" {2,}", " ")
                .trim();
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
            return objectMapper.readValue(modificationNotes, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析润色说明失败，recordNotes: {}", modificationNotes, e);
            return List.of(modificationNotes);
        }
    }
}
