package com.airesume.server.config;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.infrastructure.security.CriticalEndpointRateLimitFilter;
import com.airesume.server.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CriticalEndpointRateLimitFilter criticalEndpointRateLimitFilter;

    /**
     * CORS 配置。
     * 使用明确的 origin 列表，避免 allowCredentials(true) 与通配符冲突。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "Cache-Control"
        ));
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
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/diagnostic/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/interview/job-roles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stats").permitAll()
                        .requestMatchers("/api/resume/**").authenticated()
                        .requestMatchers("/api/interview/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(criticalEndpointRateLimitFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                        ResultCode.FORBIDDEN.getCode(), "无权限访问"))
                );

        return http.build();
    }

    /**
     * SSE/异步请求在二次分派时仍然需要走安全过滤链，否则会绕过认证与授权判断。
     * 这里显式放行 ASYNC，继续排除 ERROR，避免异常分派重复进入业务安全链。
     */
    static boolean supportsSecurityDispatcherType(DispatcherType dispatcherType) {
        return dispatcherType == DispatcherType.REQUEST
                || dispatcherType == DispatcherType.FORWARD
                || dispatcherType == DispatcherType.INCLUDE
                || dispatcherType == DispatcherType.ASYNC;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 统一写出 JSON 错误响应，供 Security 异常处理链使用。
     */
    private void writeJsonResponse(HttpServletResponse response, int httpStatus, int code, String message)
            throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(Result.error(code, message)));
    }
}
