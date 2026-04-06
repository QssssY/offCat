package com.airesume.server.service.impl;

import com.airesume.server.mock.MockDiagnosisResultGenerator;
import com.airesume.server.service.ResumeAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("resumeAiService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockResumeAiServiceImpl implements ResumeAiService {

    private final MockDiagnosisResultGenerator mockDiagnosisResultGenerator;

    @Override
    public String diagnose(String resumeText) {
        log.info("[MOCK] 简历诊断调用 (mock mode), resumeTextLength: {}",
                resumeText == null ? 0 : resumeText.length());
        return mockDiagnosisResultGenerator.generateMockDiagnosisResult("mock-file-url");
    }
}
