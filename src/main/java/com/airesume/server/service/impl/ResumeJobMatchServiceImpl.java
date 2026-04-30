package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeRequest;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeJobMatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 岗位 JD 对比分析服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeJobMatchServiceImpl extends ServiceImpl<ResumeJobMatchRecordMapper, ResumeJobMatchRecord>
        implements ResumeJobMatchService {

    /**
     * 预置跨行业关键词，覆盖技术、教育、工程、设计、商业等方向。
     */
    private static final List<String> PRESET_KEYWORDS = Arrays.asList(
            // 技术
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "RabbitMQ",
            "Docker", "Kubernetes", "Linux", "SQL", "RESTful API", "Git", "Maven", "Vue", "React",
            "TypeScript", "JavaScript", "Python", "Go", "微服务", "分布式", "高并发", "系统设计",
            "性能优化", "数据结构", "算法", "自动化测试", "CI/CD", "DevOps",
            // 教育
            "教学", "课程设计", "教案", "班主任", "学科", "教研", "公开课", "教师资格证", "学生管理",
            // 工程/制造
            "PLC", "CAD", "电气", "机械", "焊接", "电工证", "安全", "质量管理", "生产管理",
            // 设计/创意
            "UI", "UX", "Photoshop", "Illustrator", "Figma", "Sketch", "品牌", "视觉设计", "交互设计",
            // 商业/销售
            "销售", "客户", "业绩", "KPI", "市场", "营销", "品牌推广", "渠道", "商务",
            // 通用
            "团队协作", "沟通能力", "项目管理", "产品思维", "英文读写", "数据分析", "PPT", "Excel"
    );

    private static final Pattern ENGLISH_TERM_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9+#./-]{1,30}\\b");

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeJobMatchAnalyzeResponse analyzeJobMatch(Long userId, ResumeJobMatchAnalyzeRequest request) {
        Long resumeTaskId = parseResumeTaskId(request.getResumeTaskId());
        ResumeDiagnosisTask task = loadOwnedTask(userId, resumeTaskId);

        String jdText = normalizeText(request.getJdText());
        if (jdText.isBlank()) {
            throw new BusinessException("岗位 JD 文本不能为空");
        }

        String resumeText = normalizeText(request.getResumeText());
        if (resumeText.isBlank()) {
            // 兼容前端未能携带 resumeText 的场景，后端兜底从原始 PDF 提取。
            resumeText = normalizeText(pdfTextExtractor.extractText(task.getFileUrl()));
        }
        if (resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }

        List<String> targetKeywords = extractTargetKeywords(jdText);
        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        String normalizedResumeLower = resumeText.toLowerCase(Locale.ROOT);

        for (String keyword : targetKeywords) {
            if (normalizedResumeLower.contains(keyword.toLowerCase(Locale.ROOT))) {
                matchedKeywords.add(keyword);
            } else {
                missingKeywords.add(keyword);
            }
        }

        int matchScore = calculateMatchScore(targetKeywords.size(), matchedKeywords.size());
        List<String> suggestions = buildSuggestions(matchScore, matchedKeywords, missingKeywords);

        ResumeJobMatchAnalyzeResponse response = ResumeJobMatchAnalyzeResponse.builder()
                .resumeTaskId(String.valueOf(resumeTaskId))
                .matchScore(matchScore)
                .matchedKeywords(matchedKeywords)
                .missingKeywords(missingKeywords)
                .suggestions(suggestions)
                .build();

        ResumeJobMatchRecord record = new ResumeJobMatchRecord();
        record.setUserId(userId);
        record.setResumeTaskId(resumeTaskId);
        record.setResumeText(resumeText);
        record.setJdText(jdText);
        record.setMatchScore(matchScore);
        record.setAnalysisResult(toAnalysisJson(response));
        save(record);

        response.setAnalysisId(String.valueOf(record.getId()));
        response.setCreateTime(record.getCreateTime());
        return response;
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
            log.error("解析岗位 JD 对比记录失败, recordId: {}", record.getId(), e);
            return null;
        }
    }

    @Override
    public ResumeJobMatchRecord getLatestRecord(Long userId, Long resumeTaskId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
                .eq(ResumeJobMatchRecord::getResumeTaskId, resumeTaskId)
                .orderByDesc(ResumeJobMatchRecord::getCreateTime)
                .last("limit 1");
        return getOne(wrapper, false);
    }

    @Override
    public ResumeJobMatchRecord getLatestRecord(Long userId) {
        LambdaQueryWrapper<ResumeJobMatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeJobMatchRecord::getUserId, userId)
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
                .last("limit 1");
        return getOne(wrapper, false);
    }

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

    private List<String> extractTargetKeywords(String jdText) {
        Set<String> keywordSet = new LinkedHashSet<>();
        String jdLower = jdText.toLowerCase(Locale.ROOT);

        for (String keyword : PRESET_KEYWORDS) {
            if (jdLower.contains(keyword.toLowerCase(Locale.ROOT))) {
                keywordSet.add(keyword);
            }
        }

        Matcher matcher = ENGLISH_TERM_PATTERN.matcher(jdText);
        while (matcher.find()) {
            String term = matcher.group().trim();
            if (term.length() >= 2) {
                keywordSet.add(term);
            }
        }

        if (keywordSet.isEmpty()) {
            // 当 JD 过于口语化时，仍然提供一个最小可用的能力拆解。
            keywordSet.add("岗位核心技能");
            keywordSet.add("项目经验");
            keywordSet.add("业务理解");
        }

        return new ArrayList<>(keywordSet).subList(0, Math.min(keywordSet.size(), 12));
    }

    private int calculateMatchScore(int totalKeywords, int matchedCount) {
        if (totalKeywords <= 0) {
            return 0;
        }
        double ratio = (double) matchedCount / totalKeywords;
        int score = (int) Math.round(ratio * 100);
        return Math.max(0, Math.min(score, 100));
    }

    private List<String> buildSuggestions(int matchScore, List<String> matchedKeywords, List<String> missingKeywords) {
        List<String> suggestions = new ArrayList<>();

        if (!missingKeywords.isEmpty()) {
            suggestions.add("优先补充与“" + missingKeywords.get(0) + "”相关的项目经历、职责描述或结果数据。");
        }
        if (missingKeywords.size() > 1) {
            suggestions.add("将缺失关键词按“能力点 + 场景 + 结果”方式写入简历，避免只罗列名词。");
        }
        if (!matchedKeywords.isEmpty()) {
            suggestions.add("已匹配能力建议补充量化成果，例如性能提升、交付效率或业务指标变化。");
        }
        if (matchScore < 60) {
            suggestions.add("当前匹配度偏低，建议围绕岗位 JD 重新整理技能栈顺序，并强化最相关项目。");
        } else {
            suggestions.add("当前匹配基础较好，建议进一步针对岗位要求微调标题、摘要和项目关键词。");
        }

        return suggestions.stream().filter(item -> item != null && !item.isBlank()).toList();
    }

    private String toAnalysisJson(ResumeJobMatchAnalyzeResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException("岗位 JD 对比结果保存失败");
        }
    }
}
