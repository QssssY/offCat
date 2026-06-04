package com.airesume.server.service.impl;

import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TtsDiscoveryServiceImplTest {

    @Test
    void shouldBoundMimoVoiceEndpointUnavailableCacheForDifferentBaseUrls() throws Exception {
        TtsDiscoveryServiceImpl service = new TtsDiscoveryServiceImpl(
                RestClient.builder(),
                new ObjectMapper(),
                (baseUrl, timeoutMs) -> buildVoiceEndpointNotFoundRestClient(baseUrl));

        for (int i = 0; i < 180; i++) {
            String baseUrl = "https://8." + (8 + i / 250) + "." + (i % 250 + 1) + ".8/v1";
            UserTtsDiscoveryResponse response = service.discover(baseUrl, "tts-real-key", "mimo");
            assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        }

        assertTrue(readVoiceEndpointUnavailableCacheSize(service) <= 128);
    }

    @Test
    void shouldCacheMimoDiscoveryFallbackToAvoidRepeatedVoiceEndpointProbes() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        TtsDiscoveryServiceImpl service = new TtsDiscoveryServiceImpl(
                RestClient.builder(),
                new ObjectMapper(),
                (baseUrl, timeoutMs) -> builder.baseUrl(baseUrl).build());

        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "tts-real-key"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"probe\"}"));
        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("api-key", "tts-real-key"))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"mimo-v2.5-tts\"}]}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/audio/voices"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body>openresty</body></html>"));
        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/v1/audio/voices"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body>openresty</body></html>"));
        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "tts-real-key"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"probe\"}"));
        mockServer.expect(once(), requestTo("https://8.8.8.8/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("api-key", "tts-real-key"))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"mimo-v2.5-tts\"}]}",
                        MediaType.APPLICATION_JSON));

        UserTtsDiscoveryResponse first = service.discover("https://8.8.8.8/v1", "tts-real-key", "mimo");
        UserTtsDiscoveryResponse second = service.discover("https://8.8.8.8/v1", "tts-real-key", "mimo");

        assertTrue(Boolean.TRUE.equals(first.getSuccess()));
        assertTrue(Boolean.TRUE.equals(second.getSuccess()));
        assertFalse(Boolean.TRUE.equals(second.getVoiceDiscoverySupported()));
        assertEquals(first.getModels().size(), second.getModels().size());
        assertEquals(first.getVoices().size(), second.getVoices().size());
        mockServer.verify();
    }

    private RestClient buildVoiceEndpointNotFoundRestClient(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory((uri, method) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
                    if (HttpMethod.POST.equals(method) && uri.getPath().endsWith("/chat/completions")) {
                        request.setResponse(jsonResponse("{\"error\":\"probe\"}", HttpStatus.BAD_REQUEST));
                        return request;
                    }
                    if (HttpMethod.GET.equals(method) && uri.getPath().endsWith("/models")) {
                        request.setResponse(jsonResponse("{\"data\":[{\"id\":\"mimo-v2.5-tts\"}]}",
                                HttpStatus.OK));
                        return request;
                    }
                    request.setResponse(jsonResponse("{\"error\":\"not found\"}", HttpStatus.NOT_FOUND));
                    return request;
                })
                .build();
    }

    private MockClientHttpResponse jsonResponse(String body, HttpStatus status) {
        MockClientHttpResponse response = new MockClientHttpResponse(
                body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

    private int readVoiceEndpointUnavailableCacheSize(TtsDiscoveryServiceImpl service) throws Exception {
        Field field = TtsDiscoveryServiceImpl.class.getDeclaredField("voiceEndpointUnavailableCache");
        field.setAccessible(true);
        ConcurrentMap<?, ?> cache = (ConcurrentMap<?, ?>) field.get(service);
        return cache.size();
    }
}
