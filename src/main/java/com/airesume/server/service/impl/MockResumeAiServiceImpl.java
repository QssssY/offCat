package com.airesume.server.service.impl;

import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;
import com.airesume.server.mock.MockDiagnosisResultGenerator;
import com.airesume.server.service.ResumeAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 简历 AI Mock 服务实现。
 */
@Service("resumeAiService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockResumeAiServiceImpl implements ResumeAiService {

    private final MockDiagnosisResultGenerator mockDiagnosisResultGenerator;

    @Override
    public String diagnose(String resumeText) {
        log.info("[MOCK] 简历诊断调用(mock mode), resumeTextLength: {}",
                resumeText == null ? 0 : resumeText.length());
        return mockDiagnosisResultGenerator.generateMockDiagnosisResult("mock-file-url");
    }

    @Override
    public ResumePolishAiResult polishResume(String resumeText, String jdText, ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        log.info("[MOCK] AI 简历润色调用(mock mode), resumeTextLength: {}, hasJd: {}",
                resumeText == null ? 0 : resumeText.length(),
                jdText != null && !jdText.isBlank());

        StringBuilder polishedTextBuilder = new StringBuilder();
        polishedTextBuilder.append("【求职摘要】\n");
        polishedTextBuilder.append("具备较完整的项目经历与技术实践，能够围绕岗位目标整理经历并突出业务结果。\n\n");
        polishedTextBuilder.append("【核心能力】\n");
        polishedTextBuilder.append("1. 将目标岗位相关技能前置展示，提升招聘方快速阅读效率。\n");
        polishedTextBuilder.append("2. 使用“场景-动作-结果”方式改写项目表达，增强成果导向。\n");
        polishedTextBuilder.append("3. 保留原始经历事实，只优化结构、措辞和投递针对性。\n\n");
        polishedTextBuilder.append("【项目经历优化示例】\n");
        polishedTextBuilder.append("- 负责核心模块设计与开发，推动关键功能按期交付，并通过性能优化提升系统稳定性。\n");
        polishedTextBuilder.append("- 围绕复杂业务场景拆解技术方案，持续沉淀可复用能力与协作流程。");

        if (jdText != null && !jdText.isBlank()) {
            polishedTextBuilder.append("\n\n【岗位定向补充】\n");
            polishedTextBuilder.append("已结合岗位 JD 强化技能排序、项目关键词与结果表达。");
        }

        ResumePolishAiResult result = new ResumePolishAiResult();
        result.setPolishedResumeText(polishedTextBuilder.toString().trim());
        result.setModificationNotes(jdText != null && !jdText.isBlank()
                ? List.of(
                "结合最近一次岗位 JD 分析结果，优先突出与目标岗位最相关的技能和项目。",
                "将原有经历改写为更适合投递阅读的结构，增强结果导向表达。",
                "补充更明确的职责和价值描述，减少泛化表述。"
        )
                : List.of(
                "对原简历内容进行了结构化重写，提升摘要、能力和项目表达的清晰度。",
                "保留原始事实信息，重点优化了投递语气和成果呈现方式。",
                "将项目描述改写为更适合招聘方快速阅读的结果导向表达。"
        ));
        return result;
    }
}
