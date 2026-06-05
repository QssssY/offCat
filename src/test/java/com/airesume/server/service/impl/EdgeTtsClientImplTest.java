package com.airesume.server.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocketHandshakeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class EdgeTtsClientImplTest {

    @Test
    void shouldUseCurrentEdgeTtsProtocolVersionInWebSocketUri() {
        EdgeTtsClientImpl client = new EdgeTtsClientImpl();

        URI uri = client.buildWebSocketUri(null);

        assertTrue(uri.toString().contains("Sec-MS-GEC-Version=1-143.0.3650.75"));
        assertFalse(uri.toString().contains("1-130.0.2849.68"));
    }

    @Test
    void shouldSendEdgeTtsMuidCookieAndCurrentBrowserHeaders() {
        EdgeTtsClientImpl client = new EdgeTtsClientImpl();

        Map<String, String> headers = client.buildWebSocketHeaders();

        assertEquals("chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold", headers.get("Origin"));
        assertEquals("en-US,en;q=0.9", headers.get("Accept-Language"));
        assertEquals("gzip, deflate, br, zstd", headers.get("Accept-Encoding"));
        assertTrue(headers.get("User-Agent").contains("Edg/143.0.0.0"));
        assertTrue(headers.get("Cookie").matches("muid=[0-9A-F]{32};"));
    }

    @Test
    void shouldApplyEdgeTtsHeadersToJdkWebSocketBuilder() {
        EdgeTtsClientImpl client = new EdgeTtsClientImpl();
        java.net.http.WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();

        client.buildWebSocketHeaders().forEach(builder::header);
    }

    @Test
    void shouldUnwrapAsyncExecutionExceptionForEdgeTtsRootCause() {
        IllegalStateException rootCause = new IllegalStateException("Invalid response status 403");
        ExecutionException executionException = new ExecutionException(
                new CompletionException(rootCause));

        Throwable unwrapped = EdgeTtsClientImpl.unwrapAsyncFailure(executionException);

        assertSame(rootCause, unwrapped);
    }

    @Test
    void shouldResolveServerDateFromForbiddenHandshakeForRetry() {
        @SuppressWarnings("unchecked")
        HttpResponse<Object> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(403);
        when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("Date", List.of("Fri, 05 Jun 2026 08:00:00 GMT")),
                (name, value) -> true));
        when(response.request()).thenReturn(mock(HttpRequest.class));
        WebSocketHandshakeException exception = new WebSocketHandshakeException(response);

        Optional<Instant> retryInstant = EdgeTtsClientImpl.resolveForbiddenHandshakeServerDate(exception);

        assertTrue(retryInstant.isPresent());
        assertEquals(Instant.parse("2026-06-05T08:00:00Z"), retryInstant.get());
    }
}
