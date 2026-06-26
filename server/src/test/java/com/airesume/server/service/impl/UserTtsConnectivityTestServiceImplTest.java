package com.airesume.server.service.impl;

import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.service.EdgeTtsClient;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTtsConnectivityTestServiceImplTest {

    @Test
    void shouldRejectChatCompletionsTtsConnectivityWhenAudioDataMissing() {
        RestClientChain chain = RestClientChain.returning("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildMimoRequest());

        assertFalse(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("API_ERROR", response.getErrorType());
        verify(chain.uriSpec).uri("/chat/completions");
        verify(chain.bodySpec).header("api-key", "tts-real-key");
    }

    @Test
    void shouldAcceptChatCompletionsTtsConnectivityWithValidAudioData() {
        String audioData = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        RestClientChain chain = RestClientChain.returning(
                "{\"choices\":[{\"message\":{\"audio\":{\"data\":\"" + audioData + "\"}}}]}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildMimoRequest());

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("/chat/completions", response.getEndpointPath());
        verify(chain.clonedBuilder).baseUrl("https://8.8.8.8/v1");
        verify(chain.uriSpec).uri("/chat/completions");
        verify(chain.bodySpec).header("api-key", "tts-real-key");
    }

    @Test
    void shouldTestEdgeTtsConnectivityWithoutApiKey() {
        EdgeTtsClient edgeTtsClient = mock(EdgeTtsClient.class);
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                mock(RestClient.Builder.class), edgeTtsClient);
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl("https://speech.platform.bing.com");
        request.setModel("edge-tts");
        request.setVoiceId("zh-CN-XiaoxiaoNeural");
        request.setTtsProvider("edge");
        when(edgeTtsClient.synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(10000)))
                .thenReturn(new byte[]{1, 2, 3});

        UserTtsConnectivityTestResponse response = service.testConnectivity(request);

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("/consumer/speech/synthesize/readaloud/edge/v1", response.getEndpointPath());
        verify(edgeTtsClient).synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(10000));
    }

    @Test
    void shouldPreviewEdgeTtsVoiceWithoutApiKey() {
        EdgeTtsClient edgeTtsClient = mock(EdgeTtsClient.class);
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                mock(RestClient.Builder.class), edgeTtsClient);
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl("https://speech.platform.bing.com");
        request.setModel("edge-tts");
        request.setVoiceId("zh-CN-XiaoxiaoNeural");
        request.setTtsProvider("edge");
        when(edgeTtsClient.synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(10000)))
                .thenReturn(new byte[]{4, 5, 6});

        byte[] audio = service.previewVoice(request);

        assertEquals(3, audio.length);
        verify(edgeTtsClient).synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(10000));
    }

    @Test
    void shouldPreviewGeminiTtsAsWavAudio() {
        String pcmData = Base64.getEncoder().encodeToString(new byte[]{1, 0, 2, 0});
        RestClientChain chain = RestClientChain.returning(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"mimeType\":\"audio/L16;codec=pcm;rate=24000\",\"data\":\""
                        + pcmData + "\"}}]}}]}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        TtsAudioResult audio = service.previewVoiceAudio(buildProviderRequest(
                "gemini",
                "https://generativelanguage.googleapis.com",
                "gemini-key",
                "gemini-2.5-flash-preview-tts",
                "Kore",
                "/v1beta/models/{model}:generateContent"));

        assertEquals("audio/wav", audio.getContentType());
        assertEquals('R', audio.getAudioBytes()[0]);
        assertEquals('I', audio.getAudioBytes()[1]);
        verify(chain.uriSpec).uri("/v1beta/models/gemini-2.5-flash-preview-tts:generateContent");
        verify(chain.bodySpec).header("x-goog-api-key", "gemini-key");
    }

    @Test
    void shouldDecodeMiniMaxHexAudioForPreview() {
        RestClientChain chain = RestClientChain.returning("{\"data\":{\"audio\":\"010203\"}}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        TtsAudioResult audio = service.previewVoiceAudio(buildProviderRequest(
                "minimax",
                "https://api.minimax.chat",
                "mini-key",
                "speech-02-turbo",
                "male-qn-qingse",
                "/v1/t2a_v2"));

        assertEquals("audio/mpeg", audio.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, audio.getAudioBytes());
        verify(chain.uriSpec).uri("/v1/t2a_v2");
        verify(chain.bodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer mini-key");
    }

    @Test
    void shouldRejectQwenTtsAudioUrlOutsideAliyunDomain() {
        RestClientChain chain = RestClientChain.returning(
                "{\"output\":{\"audio\":{\"url\":\"https://evil.example.com/tts.mp3\"}}}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildProviderRequest(
                "qwen",
                "https://dashscope.aliyuncs.com",
                "dash-key",
                "qwen3-tts-flash",
                "Cherry",
                "/api/v1/services/aigc/multimodal-generation/generation"));

        assertFalse(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("API_ERROR", response.getErrorType());
        verify(chain.uriSpec).uri("/api/v1/services/aigc/multimodal-generation/generation");
        verify(chain.bodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer dash-key");
    }

    @Test
    void shouldAcceptQwenOfficialHttpOssAudioUrlForConnectivity() {
        RestClientChain chain = RestClientChain.returning(
                "{\"output\":{\"audio\":{\"url\":\"http://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/qwen/demo.wav\"}}}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildProviderRequest(
                "qwen",
                "https://dashscope.aliyuncs.com",
                "dash-key",
                "qwen3-tts-flash",
                "Cherry",
                "/api/v1/services/aigc/multimodal-generation/generation"));

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("/api/v1/services/aigc/multimodal-generation/generation", response.getEndpointPath());
        verify(chain.uriSpec).uri("/api/v1/services/aigc/multimodal-generation/generation");
        verify(chain.bodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer dash-key");
    }

    @Test
    void shouldCallXaiTtsWithoutSendingModelField() {
        RestClientChain chain = RestClientChain.returningBytes(new byte[]{7, 8, 9});
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(
                chain.rootBuilder, mock(EdgeTtsClient.class));

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildProviderRequest(
                "xai",
                "https://api.x.ai",
                "xai-key",
                "grok-tts",
                "Fritz-PlayAI",
                "/v1/tts"));

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        var captor = forClass(Object.class);
        verify(chain.bodySpec).body(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue();
        assertFalse(body.containsKey("model"));
        assertEquals("你好", body.get("input"));
        assertEquals("Fritz-PlayAI", body.get("voice"));
        verify(chain.uriSpec).uri("/v1/tts");
        verify(chain.bodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer xai-key");
    }

    private UserTtsConnectivityTestRequest buildMimoRequest() {
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl("https://8.8.8.8/v1");
        request.setApiKey("tts-real-key");
        request.setModel("mimo-v2.5-tts");
        request.setVoiceId("mimo_default");
        request.setTtsProvider("mimo");
        return request;
    }

    private UserTtsConnectivityTestRequest buildProviderRequest(
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            String voiceId,
            String endpointPath) {
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl(baseUrl);
        request.setApiKey(apiKey);
        request.setModel(model);
        request.setVoiceId(voiceId);
        request.setEndpointPath(endpointPath);
        request.setTtsProvider(provider);
        return request;
    }

    private static final class RestClientChain {
        private final RestClient.Builder rootBuilder;
        private final RestClient.Builder clonedBuilder;
        private final RestClient.RequestBodyUriSpec uriSpec;
        private final RestClient.RequestBodySpec bodySpec;

        private RestClientChain(
                RestClient.Builder rootBuilder,
                RestClient.Builder clonedBuilder,
                RestClient.RequestBodyUriSpec uriSpec,
                RestClient.RequestBodySpec bodySpec) {
            this.rootBuilder = rootBuilder;
            this.clonedBuilder = clonedBuilder;
            this.uriSpec = uriSpec;
            this.bodySpec = bodySpec;
        }

        private static RestClientChain returning(String responseJson) {
            RestClient.Builder rootBuilder = mock(RestClient.Builder.class);
            RestClient.Builder clonedBuilder = mock(RestClient.Builder.class);
            RestClient restClient = mock(RestClient.class);
            RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(rootBuilder.clone()).thenReturn(clonedBuilder);
            when(clonedBuilder.baseUrl(anyString())).thenReturn(clonedBuilder);
            when(clonedBuilder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(clonedBuilder);
            when(clonedBuilder.defaultHeader(eq(HttpHeaders.CONTENT_TYPE), any(String[].class)))
                    .thenReturn(clonedBuilder);
            when(clonedBuilder.build()).thenReturn(restClient);
            when(restClient.post()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), any(String[].class))).thenReturn(bodySpec);
            when(bodySpec.accept(any(org.springframework.http.MediaType[].class))).thenReturn(bodySpec);
            when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(responseJson);
            return new RestClientChain(rootBuilder, clonedBuilder, uriSpec, bodySpec);
        }

        private static RestClientChain returningBytes(byte[] responseBytes) {
            RestClientChain chain = returning("{}");
            RestClient.ResponseSpec responseSpec = chain.bodySpec.retrieve();
            when(responseSpec.body(byte[].class)).thenReturn(responseBytes);
            return chain;
        }
    }
}
