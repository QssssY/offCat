package com.airesume.server.service.impl;

import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTtsConnectivityTestServiceImplTest {

    @Test
    void shouldRejectChatCompletionsTtsConnectivityWhenAudioDataMissing() {
        RestClientChain chain = RestClientChain.returning("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}");
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(chain.rootBuilder);

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
        UserTtsConnectivityTestServiceImpl service = new UserTtsConnectivityTestServiceImpl(chain.rootBuilder);

        UserTtsConnectivityTestResponse response = service.testConnectivity(buildMimoRequest());

        assertTrue(Boolean.TRUE.equals(response.getSuccess()));
        assertEquals("/chat/completions", response.getEndpointPath());
        verify(chain.clonedBuilder).baseUrl("https://8.8.8.8/v1");
        verify(chain.uriSpec).uri("/chat/completions");
        verify(chain.bodySpec).header("api-key", "tts-real-key");
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
    }
}
