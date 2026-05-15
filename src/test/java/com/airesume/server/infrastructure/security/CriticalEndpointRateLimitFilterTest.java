package com.airesume.server.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CriticalEndpointRateLimitFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBlockRegisterRequestAfterLimitReached() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/auth/register", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = buildRequest("POST", "/api/auth/register", "10.0.0.1");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        verify(blockedChain, never()).doFilter(blockedRequest, blockedResponse);
        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void shouldBlockResetPasswordRequestAfterLimitReached() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/auth/reset-password", "10.0.0.8");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = buildRequest("POST", "/api/auth/reset-password", "10.0.0.8");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        verify(blockedChain, never()).doFilter(blockedRequest, blockedResponse);
        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void shouldBlockSecurityQuestionLookupAfterLimitReached() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = buildRequest("GET", "/api/auth/security-question", "10.0.0.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = buildRequest("GET", "/api/auth/security-question", "10.0.0.9");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        verify(blockedChain, never()).doFilter(blockedRequest, blockedResponse);
        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void shouldUseAuthenticatedUserAsInterviewRateLimitKey() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null));
        for (int i = 0; i < 40; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/interview/session/abc/message", "10.0.0.2");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = buildRequest("POST", "/api/interview/session/abc/message", "10.0.0.2");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);
        filter.doFilter(blockedRequest, blockedResponse, blockedChain);
        verify(blockedChain, never()).doFilter(blockedRequest, blockedResponse);
        assertEquals(429, blockedResponse.getStatus());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(101L, null));
        MockHttpServletRequest anotherUserRequest = buildRequest("POST", "/api/interview/session/abc/message", "10.0.0.2");
        MockHttpServletResponse anotherUserResponse = new MockHttpServletResponse();
        FilterChain anotherUserChain = mock(FilterChain.class);

        filter.doFilter(anotherUserRequest, anotherUserResponse, anotherUserChain);

        verify(anotherUserChain).doFilter(anotherUserRequest, anotherUserResponse);
        assertEquals(200, anotherUserResponse.getStatus());
    }

    @Test
    void shouldAllowRequestsAgainAfterWindowExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(buildRequest("POST", "/api/auth/register", "10.0.0.3"),
                    new MockHttpServletResponse(), mock(FilterChain.class));
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(buildRequest("POST", "/api/auth/register", "10.0.0.3"),
                blockedResponse, mock(FilterChain.class));
        assertEquals(429, blockedResponse.getStatus());

        clock.plus(Duration.ofMinutes(16));

        MockHttpServletRequest requestAfterWindow = buildRequest("POST", "/api/auth/register", "10.0.0.3");
        MockHttpServletResponse responseAfterWindow = new MockHttpServletResponse();
        FilterChain chainAfterWindow = mock(FilterChain.class);

        filter.doFilter(requestAfterWindow, responseAfterWindow, chainAfterWindow);

        verify(chainAfterWindow).doFilter(requestAfterWindow, responseAfterWindow);
        assertEquals(200, responseAfterWindow.getStatus());
    }

    @Test
    void shouldIgnoreForwardedForWhenProxyTrustIsDisabled() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        CriticalEndpointRateLimitFilter filter = new CriticalEndpointRateLimitFilter(new ObjectMapper(), clock);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/auth/register", "10.0.0.4");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        MockHttpServletRequest blockedRequest = buildRequest("POST", "/api/auth/register", "10.0.0.4");
        blockedRequest.addHeader("X-Forwarded-For", "198.51.100.99");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        verify(blockedChain, never()).doFilter(blockedRequest, blockedResponse);
        assertEquals(429, blockedResponse.getStatus());
    }

    private MockHttpServletRequest buildRequest(String method, String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void plus(Duration duration) {
            current = current.plus(duration);
        }
    }
}
