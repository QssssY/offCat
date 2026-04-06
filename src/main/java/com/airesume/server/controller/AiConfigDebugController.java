package com.airesume.server.controller;

import com.airesume.server.service.ResumeAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 配置调试控制器
 * 用于验证运行时 AI 配置状态，仅用于开发调试
 */
@Slf4j
@RestController
@RequestMapping("/api/debug/ai-config")
@RequiredArgsConstructor
public class AiConfigDebugController {

    private final Environment environment;

    @Autowired(required = false)
    private ResumeAiService resumeAiService;

    @Value("${app.ai.mode:}")
    private String aiMode;

    @Value("${app.ai.provider:}")
    private String aiProvider;

    @Value("${app.ai.model:}")
    private String aiModel;

    @Value("${app.ai.base-url:}")
    private String aiBaseUrl;

    /**
     * 获取当前 AI 配置状态
     */
    @GetMapping
    public Map<String, Object> getAiConfigStatus() {
        Map<String, Object> result = new HashMap<>();

        // 1. 配置文件值
        Map<String, String> config = new HashMap<>();
        config.put("app.ai.mode", aiMode);
        config.put("app.ai.provider", aiProvider);
        config.put("app.ai.model", aiModel);
        config.put("app.ai.base-url", aiBaseUrl);
        result.put("config", config);

        // 2. 当前激活的 Profile
        String[] activeProfiles = environment.getActiveProfiles();
        result.put("activeProfiles", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"});

        // 3. AI 服务实现类
        Map<String, Object> serviceInfo = new HashMap<>();
        if (resumeAiService != null) {
            serviceInfo.put("beanName", "resumeAiService");
            serviceInfo.put("implementationClass", resumeAiService.getClass().getName());
            serviceInfo.put("isMock", resumeAiService.getClass().getSimpleName().contains("Mock"));
            serviceInfo.put("isDoubao", resumeAiService.getClass().getSimpleName().contains("Doubao"));
        } else {
            serviceInfo.put("error", "ResumeAiService bean not found");
        }
        result.put("resumeAiService", serviceInfo);

        // 4. 环境变量（API Key 相关，只显示是否存在，不显示值）
        Map<String, Boolean> envKeys = new HashMap<>();
        envKeys.put("DOUBAO_API_KEY", System.getenv("DOUBAO_API_KEY") != null);
        envKeys.put("API_KEY", System.getenv("API_KEY") != null);
        envKeys.put("AI_API_KEY", System.getenv("AI_API_KEY") != null);
        result.put("environmentVariables", envKeys);

        // 5. 判定结论
        Map<String, Object> conclusion = new HashMap<>();
        boolean isRealMode = "real".equalsIgnoreCase(aiMode);
        boolean isDoubaoProvider = "doubao".equalsIgnoreCase(aiProvider);
        boolean hasDoubaoImpl = resumeAiService != null && resumeAiService.getClass().getSimpleName().contains("Doubao");
        boolean hasApiKey = System.getenv("DOUBAO_API_KEY") != null ||
                            System.getenv("API_KEY") != null ||
                            System.getenv("AI_API_KEY") != null;

        conclusion.put("isRealMode", isRealMode);
        conclusion.put("isDoubaoProvider", isDoubaoProvider);
        conclusion.put("hasDoubaoImplementation", hasDoubaoImpl);
        conclusion.put("hasApiKeySet", hasApiKey);
        conclusion.put("shouldUseDoubao", isRealMode && isDoubaoProvider && hasDoubaoImpl);
        conclusion.put("readyForDoubao", isRealMode && isDoubaoProvider && hasDoubaoImpl && hasApiKey);

        result.put("conclusion", conclusion);

        log.info("AI 配置状态检查: realMode={}, doubaoProvider={}, hasDoubaoImpl={}, hasApiKey={}",
                isRealMode, isDoubaoProvider, hasDoubaoImpl, hasApiKey);

        return result;
    }
}
