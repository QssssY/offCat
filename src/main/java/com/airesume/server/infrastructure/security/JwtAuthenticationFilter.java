package com.airesume.server.infrastructure.security;

import com.airesume.server.entity.SysUser;
import com.airesume.server.service.SysUserService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final SysUserService sysUserService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ERROR;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // 异步分派也要补齐 JWT 认证，避免 SSE 等链路在 ASYNC 阶段丢失鉴权上下文。
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                Integer role = jwtUtil.getRoleFromToken(token);
                SysUser user = sysUserService.getById(userId);
                // 注销或禁用账号即使 token 未过期，也不能继续写入认证上下文。
                if (user == null
                        || Integer.valueOf(1).equals(user.getIsDeleted())
                        || !Integer.valueOf(1).equals(user.getStatus())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String roleName = getRoleName(role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(roleName))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.warn("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getPrefix())) {
            return bearerToken.substring(jwtProperties.getPrefix().length());
        }
        return null;
    }

    private String getRoleName(Integer role) {
        return switch (role) {
            case 9 -> "ROLE_ADMIN";
            case 1 -> "ROLE_VIP";
            default -> "ROLE_USER";
        };
    }

}
