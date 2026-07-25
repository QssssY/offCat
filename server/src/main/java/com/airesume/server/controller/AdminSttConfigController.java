package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminSttConfigRequest;
import com.airesume.server.dto.admin.AdminSttConfigResponse;
import com.airesume.server.dto.admin.AdminSttConnectivityTestResponse;
import com.airesume.server.service.SysSttConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统级 STT（语音识别）配置接口。
 * <p>
 * 只服务语音面试的语音输入兜底：浏览器 Web Speech 不可用时改用云端识别。
 * 与面试对话 AI、语音播报 TTS 的配置完全独立。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stt-config")
@RequiredArgsConstructor
public class AdminSttConfigController {

    private final SysSttConfigService sysSttConfigService;

    /**
     * 查询系统 STT 配置，API Key 只返回脱敏值。
     */
    @GetMapping
    public Result<AdminSttConfigResponse> getConfig() {
        return Result.success(sysSttConfigService.getCurrentConfig());
    }

    /**
     * 保存系统 STT 配置。
     */
    @PutMapping
    public Result<AdminSttConfigResponse> saveConfig(@Valid @RequestBody AdminSttConfigRequest request) {
        log.info("Admin save system STT config, enabled: {}, model: {}",
                request.getEnabled(), request.getModel());
        AdminSttConfigResponse response = sysSttConfigService.saveConfig(request);
        return Result.success("系统 STT 配置已保存", response);
    }

    /**
     * 测试当前表单中的系统 STT 连通性，不保存配置。
     */
    @PostMapping("/test-connectivity")
    public Result<AdminSttConnectivityTestResponse> testConnectivity(
            @Valid @RequestBody AdminSttConfigRequest request) {
        AdminSttConnectivityTestResponse response = sysSttConfigService.testConnectivity(request);
        return Result.success(response.getMessage(), response);
    }
}
