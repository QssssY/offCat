package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminTtsConfigRequest;
import com.airesume.server.dto.admin.AdminTtsConfigResponse;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.service.SysTtsConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统级 TTS 配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tts-config")
@RequiredArgsConstructor
public class AdminTtsConfigController {

    private final SysTtsConfigService sysTtsConfigService;

    /**
     * 查询系统 TTS 配置，API Key 只返回脱敏值。
     */
    @GetMapping
    public Result<AdminTtsConfigResponse> getConfig() {
        return Result.success(sysTtsConfigService.getCurrentConfig());
    }

    /**
     * 保存系统 TTS 配置。
     */
    @PutMapping
    public Result<AdminTtsConfigResponse> saveConfig(@Valid @RequestBody AdminTtsConfigRequest request) {
        log.info("Admin save system TTS config, enabled: {}, provider: {}, model: {}",
                request.getEnabled(), request.getTtsProvider(), request.getModel());
        AdminTtsConfigResponse response = sysTtsConfigService.saveConfig(request);
        return Result.success("系统 TTS 配置已保存", response);
    }

    /**
     * 测试当前表单中的系统 TTS 连通性，不保存配置。
     */
    @PostMapping("/test-connectivity")
    public Result<UserTtsConnectivityTestResponse> testConnectivity(
            @Valid @RequestBody AdminTtsConfigRequest request) {
        UserTtsConnectivityTestResponse response = sysTtsConfigService.testConnectivity(request);
        return Result.success(response.getMessage(), response);
    }

    /**
     * 使用当前表单参数发现可用模型和音色，不保存配置。
     */
    @PostMapping("/discover")
    public Result<UserTtsDiscoveryResponse> discover(@Valid @RequestBody AdminTtsConfigRequest request) {
        UserTtsDiscoveryResponse response = sysTtsConfigService.discover(request);
        return Result.success(response.getMessage(), response);
    }

    /**
     * 使用当前表单参数试听音色，按 Provider 返回真实音频媒体类型。
     */
    @PostMapping(value = "/preview")
    public ResponseEntity<byte[]> previewVoice(@Valid @RequestBody AdminTtsConfigRequest request) {
        TtsAudioResult audio = sysTtsConfigService.previewVoiceAudio(request);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(audio.getContentType()))
                .cacheControl(CacheControl.noStore())
                .body(audio.getAudioBytes());
    }
}
