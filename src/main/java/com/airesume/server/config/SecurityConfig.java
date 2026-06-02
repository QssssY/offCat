
package com.airesume.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.infrastructure.security.CriticalEndpointRateLimitFilter;
import com.airesume.server.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PUBLIC_UPLOAD_PATTERN = "/uploads/community/**";
    private static final String AUTH_REGISTER_PATH = "/api/auth/register";
    private static final String AUTH_LOGIN_PATH = "/api/auth/login";
    private static final String AUTH_RESET_PASSWORD_PATH = "/api/auth/reset-password";
    private static final String AUTH_SECURITY_QUESTION_PATH = "/api/auth/security-question";
    private static final String AUTH_CAPTCHA_PATH = "/api/auth/captcha";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CriticalEndpointRateLimitFilter criticalEndpointRateLimitFilter;

    /**
     * CORS配置源
     * 解决CORS配置冲突：allowCredentials(true)不能与通配符*同时使用
     * 使用具体origin列表，支持从环境变量或配置文件读取
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 从环境变量读取允许的origin，默认允许localhost:3000（开发环境）
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            // 环境变量配置多个origin，用逗号分隔
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            // 默认开发环境配置
            configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher((HttpServletRequest request) ->
                        supportsSecurityDispatcherType(request.getDispatcherType()))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 认证模块只开放注册、登录和找回密码入口；当前用户、改密等接口必须有有效 JWT。
                        .requestMatchers(HttpMethod.POST,
                                AUTH_REGISTER_PATH,
                                AUTH_LOGIN_PATH,
                                AUTH_RESET_PASSWORD_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, AUTH_SECURITY_QUESTION_PATH,
                                AUTH_CAPTCHA_PATH).permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 静态资源（上传的图片等）放行 - img标签不会携带JWT token
                        .requestMatchers(PUBLIC_UPLOAD_PATTERN).permitAll()
                        // 网络诊断会暴露代理、DNS、端口等运行环境信息，只允许管理员排查。
                        .requestMatchers("/api/diagnostic/**").hasRole("ADMIN")
                        // 用户端岗位选项需要由后台配置提供，前端不能再写死，所以这里开放只读岗位列表。
                        .requestMatchers(HttpMethod.GET, "/api/interview/job-roles").permitAll()
                        // 公开统计接口放行 - 首页展示平台数据
                        .requestMatchers(HttpMethod.GET, "/api/stats").permitAll()
                        .requestMatchers("/api/resume/**").authenticated()
                        .requestMatchers("/api/interview/**").authenticated()
                        // 用户引导等个人功能需要登录
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 关键高成本接口限流依赖 JWT 写入的认证上下文，必须挂在 JWT 过滤器之后。
                .addFilterAfter(criticalEndpointRateLimitFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        // 未认证：返回 401 JSON 而非默认重定向
                        .authenticationEntryPoint((request, response, authException) -> {
                            writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                    ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
                        })
                        // 已认证但权限不足：返回 403 JSON
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                    ResultCode.FORBIDDEN.getCode(), "无权限访问");
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器只处理正常请求链路，排除 ERROR 分发避免错误页二次触发认证。
     */
    public static boolean supportsSecurityDispatcherType(DispatcherType dispatcherType) {
        return dispatcherType == DispatcherType.REQUEST
                || dispatcherType == DispatcherType.FORWARD
                || dispatcherType == DispatcherType.INCLUDE
                || dispatcherType == DispatcherType.ASYNC;
    }

    /**
     * 判断上传路径是否属于可公开访问的社区图片目录。
     */
    static boolean supportsPublicUploadPath(String path) {
        return path != null && PATH_MATCHER.match(PUBLIC_UPLOAD_PATTERN, path);
    }

    /**
     * 判断认证模块接口是否允许匿名访问，避免把 /api/auth/me 等登录态接口误放行。
     */
    static boolean supportsPublicAuthEndpoint(HttpMethod method, String path) {
        if (method == null || path == null) {
            return false;
        }
        if (method == HttpMethod.POST) {
            return AUTH_REGISTER_PATH.equals(path)
                    || AUTH_LOGIN_PATH.equals(path)
                    || AUTH_RESET_PASSWORD_PATH.equals(path);
        }
        return method == HttpMethod.GET && (AUTH_SECURITY_QUESTION_PATH.equals(path)
                || AUTH_CAPTCHA_PATH.equals(path));
    }

    /**
     * 写入标准 JSON 错误响应（供 Security 异常处理使用）
     */
    private void writeJsonResponse(HttpServletResponse response, int httpStatus, int code, String message) throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        byte[] body = mapper.writeValueAsBytes(Result.error(code, message));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.getOutputStream().flush();
    }

}
