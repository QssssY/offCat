package com.airesume.server.service.impl;

import com.airesume.server.dto.admin.AiEngineConnectivityTestRequest;
import com.airesume.server.dto.admin.AiEngineConnectivityTestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;

class AiEngineConnectivityTestServiceImplTest {

    @Test
    void shouldReturnSuccessWhenChatCompletionResponds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiEngineConnectivityTestServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer sk-real"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"ok"}}]}
                        """, MediaType.APPLICATION_JSON));

        AiEngineConnectivityTestResponse response = service.testConnectivity(buildRequest(), "sk-real");

        assertTrue(response.getSuccess());
        assertEquals("ok", response.getResponsePreview());
        assertEquals("https://8.8.8.8/v1/chat/completions", response.getEndpoint());
        server.verify();
    }

    @Test
    void shouldReturnSuccessWhenReasoningResponseHasNoFinalContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiEngineConnectivityTestServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"","reasoning_content":"checking"},"finish_reason":"length"}]}
                        """, MediaType.APPLICATION_JSON));

        AiEngineConnectivityTestResponse response = service.testConnectivity(buildRequest(), "sk-real");

        assertTrue(response.getSuccess());
        assertEquals("checking", response.getResponsePreview());
        server.verify();
    }

    @Test
    void shouldSendThinkingModeWhenConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiEngineConnectivityTestServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/chat/completions"))
                .andExpect(content().string(containsString("\"thinking\":{\"type\":\"disabled\"")))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"ok"}}]}
                        """, MediaType.APPLICATION_JSON));
        AiEngineConnectivityTestRequest request = buildRequest();
        request.setThinkingMode("disabled");

        AiEngineConnectivityTestResponse response = service.testConnectivity(request, "sk-real");

        assertTrue(response.getSuccess());
        server.verify();
    }

    @Test
    void shouldReturnFailureWhenProviderRejectsKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiEngineConnectivityTestServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/chat/completions"))
                .andRespond(withUnauthorizedRequest());

        AiEngineConnectivityTestResponse response = service.testConnectivity(buildRequest(), "bad-key");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("HTTP 401"));
        server.verify();
    }

    @Test
    void shouldReturnSuccessForMockProviderWithoutNetwork() {
        AiEngineConnectivityTestServiceImpl service =
                new AiEngineConnectivityTestServiceImpl(RestClient.builder(), new ObjectMapper());
        AiEngineConnectivityTestRequest request = buildRequest();
        request.setProviderType("mock");
        request.setBaseUrl(null);

        AiEngineConnectivityTestResponse response = service.testConnectivity(request, null);

        assertTrue(response.getSuccess());
        assertEquals("mock-ok", response.getResponsePreview());
        assertEquals("mock://local", response.getEndpoint());
    }

    @Test
    void shouldReturnFailureWithoutNetworkWhenBaseUrlIsNotPublic() {
        AiEngineConnectivityTestServiceImpl service =
                new AiEngineConnectivityTestServiceImpl(RestClient.builder(), new ObjectMapper()) {
                    @Override
                    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                        throw new AssertionError("非法基础地址不应发起外部请求");
                    }
                };
        AiEngineConnectivityTestRequest request = buildRequest();
        request.setBaseUrl("https://127.0.0.1:8080");

        AiEngineConnectivityTestResponse response = service.testConnectivity(request, "sk-real");

        assertFalse(response.getSuccess());
        assertEquals("基础地址不合法，连通测试未发起外部请求。", response.getMessage());
        assertTrue(response.getErrorMessage().contains("本机、内网或云元数据地址"));
    }

    private AiEngineConnectivityTestRequest buildRequest() {
        AiEngineConnectivityTestRequest request = new AiEngineConnectivityTestRequest();
        request.setProviderType("openai");
        request.setModelName("gpt-test");
        request.setBaseUrl("https://8.8.8.8/v1");
        request.setThinkingMode("none");
        request.setTemperature(BigDecimal.ZERO);
        request.setMaxTokens(100);
        request.setTimeoutMs(30000);
        return request;
    }

    private AiEngineConnectivityTestServiceImpl buildService(RestClient.Builder builder) {
        return new AiEngineConnectivityTestServiceImpl(builder, new ObjectMapper()) {
            @Override
            protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                return builder.clone()
                        .baseUrl(baseUrl)
                        .build();
            }
        };
    }
}
