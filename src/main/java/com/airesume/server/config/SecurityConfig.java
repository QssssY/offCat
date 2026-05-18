package com.airesume.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
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
                        request.getDispatcherType() == DispatcherType.REQUEST
                        || request.getDispatcherType() == DispatcherType.FORWARD
                        || request.getDispatcherType() == DispatcherType.INCLUDE)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // 网络诊断接口放行 - 用于排查 DNS、代理、端口等网络问题，无需登录
                        .requestMatchers("/api/diagnostic/**").permitAll()
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
     * 写入标准 JSON 错误响应（供 Security 异常处理使用）
     */
    private void writeJsonResponse(HttpServletResponse response, int httpStatus, int code, String message) throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(Result.error(code, message)));
    }

}
