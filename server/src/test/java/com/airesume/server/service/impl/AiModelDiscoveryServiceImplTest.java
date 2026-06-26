package com.airesume.server.service.impl;

import com.airesume.server.dto.ai.AiModelDiscoveryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class AiModelDiscoveryServiceImplTest {

    @Test
    void shouldReturnDistinctModelsWhenProviderReturnsOpenAiCompatibleList() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelDiscoveryServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/models"))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, "Bearer sk-real"))
                .andRespond(withSuccess("""
                        {"data":[{"id":"gpt-4o-mini"},{"id":"gpt-4o"},{"id":"gpt-4o-mini"},{"id":""}]}
                        """, MediaType.APPLICATION_JSON));

        AiModelDiscoveryResponse response = service.fetchModels("https://8.8.8.8/v1", "sk-real", 10000, "openai");

        assertTrue(response.getSuccess());
        assertEquals(2, response.getModels().size());
        assertEquals("gpt-4o-mini", response.getModels().get(0).getId());
        assertEquals("gpt-4o", response.getModels().get(1).getName());
        server.verify();
    }

    @Test
    void shouldReturnFailureWhenProviderRejectsKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelDiscoveryServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/models"))
                .andRespond(withUnauthorizedRequest());

        AiModelDiscoveryResponse response = service.fetchModels("https://8.8.8.8/v1", "bad-key", 10000, "openai");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("HTTP 401"));
        server.verify();
    }

    @Test
    void shouldHideRawHtmlBodyWhenProviderModelEndpointNotFound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelDiscoveryServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/models"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("""
                                <html>
                                <head><title>404 Not Found</title></head>
                                <body><center><h1>404 Not Found</h1></center><hr><center>openresty</center></body>
                                </html>
                                """)
                        .contentType(MediaType.TEXT_HTML));

        AiModelDiscoveryResponse response = service.fetchModels("https://8.8.8.8/v1", "sk-real", 10000, "openai");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("HTTP 404"));
        assertTrue(response.getErrorMessage().contains("基础地址"));
        assertTrue(response.getErrorMessage().contains("手动输入模型名"));
        assertFalse(response.getErrorMessage().contains("<html>"));
        assertFalse(response.getErrorMessage().contains("openresty"));
        server.verify();
    }

    @Test
    void shouldReturnFailureWhenProviderResponseHasNoDataArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelDiscoveryServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/models"))
                .andRespond(withSuccess("{\"models\":[\"gpt-4o\"]}", MediaType.APPLICATION_JSON));

        AiModelDiscoveryResponse response = service.fetchModels("https://8.8.8.8/v1", "sk-real", 10000, "openai");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("data"));
        server.verify();
    }

    @Test
    void shouldReturnFailureWhenProviderFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelDiscoveryServiceImpl service = buildService(builder);
        server.expect(requestTo("https://8.8.8.8/v1/models"))
                .andRespond(withServerError());

        AiModelDiscoveryResponse response = service.fetchModels("https://8.8.8.8/v1", "sk-real", 10000, "openai");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("HTTP 500"));
        server.verify();
    }

    @Test
    void shouldNotSendNetworkRequestWhenBaseUrlIsNotPublicHttps() {
        AiModelDiscoveryServiceImpl service =
                new AiModelDiscoveryServiceImpl(RestClient.builder(), new ObjectMapper()) {
                    @Override
                    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                        throw new AssertionError("非公网地址不应发起模型列表请求");
                    }
                };

        AiModelDiscoveryResponse response = service.fetchModels("https://127.0.0.1:8080/v1", "sk-real", 10000, "openai");

        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().contains("基础地址不合法"));
    }

    private AiModelDiscoveryServiceImpl buildService(RestClient.Builder builder) {
        return new AiModelDiscoveryServiceImpl(builder, new ObjectMapper()) {
            @Override
            protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                return builder.clone()
                        .baseUrl(baseUrl)
                        .build();
            }
        };
    }
}
