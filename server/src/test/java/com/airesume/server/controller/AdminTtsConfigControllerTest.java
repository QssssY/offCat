package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminTtsConfigRequest;
import com.airesume.server.dto.admin.AdminTtsConfigResponse;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.service.SysTtsConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTtsConfigControllerTest {

    @Test
    void shouldReturnCurrentSystemTtsConfigWithoutPlainApiKey() {
        SysTtsConfigService service = mock(SysTtsConfigService.class);
        AdminTtsConfigController controller = new AdminTtsConfigController(service);
        AdminTtsConfigResponse response = AdminTtsConfigResponse.builder()
                .enabled(true)
                .configured(true)
                .apiKey("sys****1234")
                .baseUrl("https://tts.example.com/v1")
                .model("tts-1")
                .voiceId("alloy")
                .build();
        when(service.getCurrentConfig()).thenReturn(response);

        Result<AdminTtsConfigResponse> result = controller.getConfig();

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        assertEquals("sys****1234", result.getData().getApiKey());
        verify(service).getCurrentConfig();
    }

    @Test
    void shouldSaveSystemTtsConfigThroughService() {
        SysTtsConfigService service = mock(SysTtsConfigService.class);
        AdminTtsConfigController controller = new AdminTtsConfigController(service);
        AdminTtsConfigRequest request = buildRequest();
        AdminTtsConfigResponse response = AdminTtsConfigResponse.builder()
                .enabled(true)
                .configured(true)
                .ttsProvider("openai")
                .baseUrl("https://tts.example.com/v1")
                .model("tts-1")
                .voiceId("alloy")
                .endpointPath("/audio/speech")
                .build();
        when(service.saveConfig(request)).thenReturn(response);

        Result<AdminTtsConfigResponse> result = controller.saveConfig(request);

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        verify(service).saveConfig(request);
    }

    @Test
    void shouldTestConnectivityWithSystemTtsFormValues() {
        SysTtsConfigService service = mock(SysTtsConfigService.class);
        AdminTtsConfigController controller = new AdminTtsConfigController(service);
        AdminTtsConfigRequest request = buildRequest();
        UserTtsConnectivityTestResponse response = UserTtsConnectivityTestResponse.builder()
                .success(true)
                .message("TTS 连通测试成功")
                .endpointPath("/audio/speech")
                .build();
        when(service.testConnectivity(request)).thenReturn(response);

        Result<UserTtsConnectivityTestResponse> result = controller.testConnectivity(request);

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        verify(service).testConnectivity(request);
    }

    @Test
    void shouldPreviewSystemTtsVoiceAsAudio() {
        SysTtsConfigService service = mock(SysTtsConfigService.class);
        AdminTtsConfigController controller = new AdminTtsConfigController(service);
        AdminTtsConfigRequest request = buildRequest();
        byte[] audio = new byte[]{1, 2, 3};
        when(service.previewVoiceAudio(request)).thenReturn(TtsAudioResult.of(audio, "audio/wav"));

        ResponseEntity<byte[]> response = controller.previewVoice(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("audio/wav", response.getHeaders().getContentType().toString());
        assertArrayEquals(audio, response.getBody());
        verify(service).previewVoiceAudio(request);
    }

    @Test
    void shouldDiscoverSystemTtsModelsAndVoices() {
        SysTtsConfigService service = mock(SysTtsConfigService.class);
        AdminTtsConfigController controller = new AdminTtsConfigController(service);
        AdminTtsConfigRequest request = buildRequest();
        UserTtsDiscoveryResponse response = UserTtsDiscoveryResponse.builder()
                .success(true)
                .message("模型列表获取成功")
                .build();
        when(service.discover(request)).thenReturn(response);

        Result<UserTtsDiscoveryResponse> result = controller.discover(request);

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        verify(service).discover(request);
    }

    private AdminTtsConfigRequest buildRequest() {
        AdminTtsConfigRequest request = new AdminTtsConfigRequest();
        request.setEnabled(true);
        request.setTtsProvider("openai");
        request.setBaseUrl("https://tts.example.com/v1");
        request.setApiKey("sys-real-key");
        request.setModel("tts-1");
        request.setVoiceId("alloy");
        request.setEndpointPath("/audio/speech");
        return request;
    }
}
