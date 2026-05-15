package com.airesume.server.infrastructure.security;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 关键高成本接口限流过滤器。
 * 这一层只覆盖注册、找回密码、简历上传和面试写操作，先用最小改动阻断批量滥用。
 */
@Slf4j
@Component
public class CriticalEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String SECURITY_QUESTION_PATH = "/api/auth/security-question";
    private static final String RESET_PASSWORD_PATH = "/api/auth/reset-password";
    private static final String RESUME_UPLOAD_PATH = "/api/resume/upload";
    private static final String INTERVIEW_SESSION_PATH = "/api/interview/session";
    private static final String INTERVIEW_SESSION_PREFIX = "/api/interview/session/";
    private static final long CLEANUP_INTERVAL = 256L;

    private static final RateLimitPolicy REGISTER_POLICY = new RateLimitPolicy(
            "register", REGISTER_PATH, MatchType.EXACT, 5, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY);
    private static final RateLimitPolicy SECURITY_QUESTION_POLICY = new RateLimitPolicy(
            "security_question", SECURITY_QUESTION_PATH, MatchType.EXACT, 10, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY, "GET");
    private static final RateLimitPolicy RESET_PASSWORD_POLICY = new RateLimitPolicy(
            "reset_password", RESET_PASSWORD_PATH, MatchType.EXACT, 5, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY);
    private static final RateLimitPolicy RESUME_UPLOAD_POLICY = new RateLimitPolicy(
            "resume_upload", RESUME_UPLOAD_PATH, MatchType.EXACT, 10, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    private static final RateLimitPolicy INTERVIEW_CREATE_POLICY = new RateLimitPolicy(
            "interview_create", INTERVIEW_SESSION_PATH, MatchType.EXACT, 10, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    private static final RateLimitPolicy INTERVIEW_ACTION_POLICY = new RateLimitPolicy(
            "interview_action", INTERVIEW_SESSION_PREFIX, MatchType.PREFIX, 40, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, RateLimitWindow> requestWindows = new ConcurrentHashMap<>();
    private final AtomicLong accessCounter = new AtomicLong();

    @Autowired
    public CriticalEndpointRateLimitFilter(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemDefaultZone());
    }

    CriticalEndpointRateLimitFilter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolvePolicy(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitPolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        cleanupExpiredWindowsIfNeeded(now);

        String requesterKey = buildRequesterKey(policy, request);
        String windowKey = policy.name() + "::" + requesterKey;
        RateLimitWindow window = requestWindows.compute(windowKey, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return RateLimitWindow.start(now, policy.windowMillis());
            }
            return existing.increment();
        });

        if (window.requestCount() > policy.maxRequests()) {
            log.warn("Rate limit blocked, policy: {}, key: {}, path: {}", policy.name(), requesterKey, request.getRequestURI());
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 只在固定访问间隔做一次惰性清理，避免长期运行后过期窗口无限堆积。
     */
    private void cleanupExpiredWindowsIfNeeded(long now) {
        if (accessCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        requestWindows.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * 已登录写接口优先按用户维度限流，注册和找回密码入口按来源 IP 限流。
     */
    private String buildRequesterKey(RateLimitPolicy policy, HttpServletRequest request) {
        if (policy.keyStrategy() == KeyStrategy.IP_ONLY) {
            return "ip:" + extractClientIp(request);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return "user:" + userId;
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null || request.getRemoteAddr().isBlank()
                ? "unknown"
                : request.getRemoteAddr();
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String requestMethod = request.getMethod();
        String requestUri = request.getRequestURI();
        if (REGISTER_POLICY.matches(requestMethod, requestUri)) {
            return REGISTER_POLICY;
        }
        if (SECURITY_QUESTION_POLICY.matches(requestMethod, requestUri)) {
            return SECURITY_QUESTION_POLICY;
        }
        if (RESET_PASSWORD_POLICY.matches(requestMethod, requestUri)) {
            return RESET_PASSWORD_POLICY;
        }
        if (RESUME_UPLOAD_POLICY.matches(requestMethod, requestUri)) {
            return RESUME_UPLOAD_POLICY;
        }
        if (INTERVIEW_CREATE_POLICY.matches(requestMethod, requestUri)) {
            return INTERVIEW_CREATE_POLICY;
        }
        if (INTERVIEW_ACTION_POLICY.matches(requestMethod, requestUri)) {
            return INTERVIEW_ACTION_POLICY;
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(ResultCode.TOO_MANY_REQUESTS)));
    }

    private enum MatchType {
        EXACT,
        PREFIX
    }

    private enum KeyStrategy {
        IP_ONLY,
        USER_OR_IP
    }

    private record RateLimitPolicy(String name,
                                   String path,
                                   MatchType matchType,
                                   int maxRequests,
                                   long windowMillis,
                                   KeyStrategy keyStrategy,
                                   String method) {

        private RateLimitPolicy(String name,
                                String path,
                                MatchType matchType,
                                int maxRequests,
                                long windowMillis,
                                KeyStrategy keyStrategy) {
            this(name, path, matchType, maxRequests, windowMillis, keyStrategy, "POST");
        }

        private boolean matches(String requestMethod, String requestUri) {
            if (!method.equalsIgnoreCase(requestMethod)) {
                return false;
            }
            if (matchType == MatchType.EXACT) {
                return path.equals(requestUri);
            }
            return requestUri.startsWith(path);
        }
    }

    private record RateLimitWindow(int requestCount, long expireAt) {

        private static RateLimitWindow start(long now, long windowMillis) {
            return new RateLimitWindow(1, now + windowMillis);
        }

        private boolean isExpired(long now) {
            return now >= expireAt;
        }

        private RateLimitWindow increment() {
            return new RateLimitWindow(requestCount + 1, expireAt);
        }
    }
}
