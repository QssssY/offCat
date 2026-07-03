package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.user.SystemTtsStatusResponse;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.service.AiModelDiscoveryService;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserAiUsageLimitService;
import com.airesume.server.service.UserTtsSpeechService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAiConfigControllerTest {

    @Test
    void shouldReturnSystemTtsStatusWithoutExposingConfigDetail() {
        UserAiConfigService userAiConfigService = mock(UserAiConfigService.class);
        UserAiUsageLimitService userAiUsageLimitService = mock(UserAiUsageLimitService.class);
        AiModelDiscoveryService aiModelDiscoveryService = mock(AiModelDiscoveryService.class);
        UserTtsSpeechService userTtsSpeechService = mock(UserTtsSpeechService.class);
        UserAiConfigController controller = new UserAiConfigController(
                userAiConfigService,
                userAiUsageLimitService,
                aiModelDiscoveryService,
                userTtsSpeechService);
        when(userTtsSpeechService.hasSystemTtsConfig()).thenReturn(true);

        Result<SystemTtsStatusResponse> result = controller.getSystemTtsStatus();

        assertEquals(200, result.getCode());
        assertTrue(result.getData().getSystemTtsAvailable());
        verify(userTtsSpeechService).hasSystemTtsConfig();
    }

    @Test
    void shouldPreviewUserTtsVoiceWithProviderContentType() {
        UserAiConfigService userAiConfigService = mock(UserAiConfigService.class);
        UserAiUsageLimitService userAiUsageLimitService = mock(UserAiUsageLimitService.class);
        AiModelDiscoveryService aiModelDiscoveryService = mock(AiModelDiscoveryService.class);
        UserTtsSpeechService userTtsSpeechService = mock(UserTtsSpeechService.class);
        Authentication authentication = mock(Authentication.class);
        UserAiConfigController controller = new UserAiConfigController(
                userAiConfigService,
                userAiUsageLimitService,
                aiModelDiscoveryService,
                userTtsSpeechService);
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setModel("gemini-2.5-flash-preview-tts");
        request.setVoiceId("Kore");
        byte[] audio = new byte[]{1, 2, 3};
        when(authentication.getPrincipal()).thenReturn(7L);
        when(userAiConfigService.previewTtsVoiceAudio(request)).thenReturn(TtsAudioResult.of(audio, "audio/wav"));

        ResponseEntity<byte[]> response = controller.previewTtsVoice(request, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("audio/wav", response.getHeaders().getContentType().toString());
        assertArrayEquals(audio, response.getBody());
        verify(userAiConfigService).previewTtsVoiceAudio(request);
    }
}
