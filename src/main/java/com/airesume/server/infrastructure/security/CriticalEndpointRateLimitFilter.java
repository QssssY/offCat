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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.util.concurrent.TimeUnit;
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
    private static final String RESUME_EXPORT_PDF_PATH = "/api/resume/export-pdf";
    private static final String INTERVIEW_SESSION_PATH = "/api/interview/session";
    private static final String INTERVIEW_SESSION_PREFIX = "/api/interview/session/";
    private static final String INTERVIEW_STREAM_SUFFIX = "/message/stream";
    private static final String OFFER_API_PREFIX = "/api/offer/";
    private static final long CLEANUP_INTERVAL = 256L;

    private static final RateLimitPolicy REGISTER_POLICY = new RateLimitPolicy(
            "register", REGISTER_PATH, MatchType.EXACT, 5, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY);
    private static final RateLimitPolicy SECURITY_QUESTION_POLICY = new RateLimitPolicy(
            "security_question", SECURITY_QUESTION_PATH, MatchType.EXACT, 10, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY, "GET");
    private static final RateLimitPolicy RESET_PASSWORD_POLICY = new RateLimitPolicy(
            "reset_password", RESET_PASSWORD_PATH, MatchType.EXACT, 5, Duration.ofMinutes(15).toMillis(), KeyStrategy.IP_ONLY);
    private static final RateLimitPolicy RESUME_UPLOAD_POLICY = new RateLimitPolicy(
            "resume_upload", RESUME_UPLOAD_PATH, MatchType.EXACT, 10, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    // PDF 导出会启动浏览器进程生成文件，成本高于普通查询，单独限制频率保护 CPU 和内存。
    private static final RateLimitPolicy RESUME_EXPORT_PDF_POLICY = new RateLimitPolicy(
            "resume_export_pdf", RESUME_EXPORT_PDF_PATH, MatchType.EXACT, 5, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    private static final RateLimitPolicy INTERVIEW_CREATE_POLICY = new RateLimitPolicy(
            "interview_create", INTERVIEW_SESSION_PATH, MatchType.EXACT, 10, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    // 面试 SSE 流式接口会被文字和语音面试每轮对话调用，阈值需要覆盖正常多轮面试。
    // 保持独立策略便于观测和拦截异常刷接口，60 次 / 10 分钟覆盖高强度语音面试节奏。
    private static final RateLimitPolicy INTERVIEW_STREAM_POLICY = new RateLimitPolicy(
            "interview_stream", INTERVIEW_STREAM_SUFFIX, MatchType.SUFFIX, 60, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    private static final RateLimitPolicy INTERVIEW_ACTION_POLICY = new RateLimitPolicy(
            "interview_action", INTERVIEW_SESSION_PREFIX, MatchType.PREFIX, 40, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);
    private static final RateLimitPolicy OFFER_ACTION_POLICY = new RateLimitPolicy(
            "offer_action", OFFER_API_PREFIX, MatchType.PREFIX, 10, Duration.ofMinutes(10).toMillis(), KeyStrategy.USER_OR_IP);

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean trustForwardedHeaders;
    private final Map<String, RateLimitWindow> requestWindows = new ConcurrentHashMap<>();
    private final AtomicLong accessCounter = new AtomicLong();

    @Autowired
    public CriticalEndpointRateLimitFilter(ObjectMapper objectMapper,
                                           ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                           @Value("${app.security.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this(objectMapper, Clock.systemDefaultZone(), redisTemplateProvider.getIfAvailable(), trustForwardedHeaders);
    }

    CriticalEndpointRateLimitFilter(ObjectMapper objectMapper, Clock clock) {
        this(objectMapper, clock, null, false);
    }

    CriticalEndpointRateLimitFilter(ObjectMapper objectMapper,
                                    Clock clock,
                                    StringRedisTemplate stringRedisTemplate,
                                    boolean trustForwardedHeaders) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.stringRedisTemplate = stringRedisTemplate;
        this.trustForwardedHeaders = trustForwardedHeaders;
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
        long requestCount = incrementRequestCount(windowKey, policy, now);

        if (requestCount > policy.maxRequests()) {
            log.warn("Rate limit blocked, policy: {}, key: {}, path: {}", policy.name(), requesterKey, request.getRequestURI());
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private long incrementRequestCount(String windowKey, RateLimitPolicy policy, long now) {
        if (stringRedisTemplate != null) {
            try {
                String redisKey = "rate-limit:" + windowKey;
                Long count = stringRedisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1L) {
                    stringRedisTemplate.expire(redisKey, policy.windowMillis(), TimeUnit.MILLISECONDS);
                }
                if (count != null) {
                    return count;
                }
            } catch (Exception e) {
                log.warn("Redis rate limit unavailable, fallback to local window, key={}", windowKey, e);
            }
        }

        RateLimitWindow window = requestWindows.compute(windowKey, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return RateLimitWindow.start(now, policy.windowMillis());
            }
            return existing.increment();
        });
        return window.requestCount();
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
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // 仅信任最右侧（最近的）一跳代理写入的值。
                // 取最左可被任意客户端伪造："X-Forwarded-For: 1.2.3.4" 即可冒充任意 IP 绕过 IP 限频。
                String[] hops = forwardedFor.split(",");
                String rightmost = hops[hops.length - 1].trim();
                if (!rightmost.isBlank()) {
                    return rightmost;
                }
            }
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
        if (RESUME_EXPORT_PDF_POLICY.matches(requestMethod, requestUri)) {
            return RESUME_EXPORT_PDF_POLICY;
        }
        if (INTERVIEW_CREATE_POLICY.matches(requestMethod, requestUri)) {
            return INTERVIEW_CREATE_POLICY;
        }
        // 流式策略优先级高于通用 action 策略：先精确匹配 /message/stream，再回落到通用面试限频。
        if (INTERVIEW_STREAM_POLICY.matches(requestMethod, requestUri)) {
            return INTERVIEW_STREAM_POLICY;
        }
        if (INTERVIEW_ACTION_POLICY.matches(requestMethod, requestUri)) {
            return INTERVIEW_ACTION_POLICY;
        }
        if (OFFER_ACTION_POLICY.matches(requestMethod, requestUri)) {
            return OFFER_ACTION_POLICY;
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
        PREFIX,
        SUFFIX
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
            return switch (matchType) {
                case EXACT -> path.equals(requestUri);
                case PREFIX -> requestUri.startsWith(path);
                case SUFFIX -> requestUri.endsWith(path);
            };
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
