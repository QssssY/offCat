package com.airesume.server.infrastructure.security;

import com.airesume.server.entity.SysUser;
import com.airesume.server.service.SysUserService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private SysUserService sysUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAsyncDispatchRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtProperties, sysUserService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interview/session/123/message");
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(123L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("tester");
        when(jwtUtil.getRoleFromToken("valid-token")).thenReturn(0);
        SysUser user = new SysUser();
        user.setStatus(1);
        user.setIsDeleted(0);
        when(sysUserService.getById(123L)).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(123L, authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipErrorDispatchRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtProperties, sysUserService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interview/session/123/message");
        request.setDispatcherType(DispatcherType.ERROR);
        request.addHeader("Authorization", "Bearer ignored-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldRejectDisabledUserToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtProperties, sysUserService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interview/history");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(123L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("tester");
        when(jwtUtil.getRoleFromToken("valid-token")).thenReturn(0);
        SysUser user = new SysUser();
        user.setStatus(0);
        user.setIsDeleted(0);
        when(sysUserService.getById(123L)).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAutoUnbanExpiredBanAndAuthenticateToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtProperties, sysUserService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/interview/history");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtProperties.getHeader()).thenReturn("Authorization");
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(123L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("tester");
        when(jwtUtil.getRoleFromToken("valid-token")).thenReturn(0);
        SysUser user = new SysUser();
        user.setId(123L);
        user.setStatus(0);
        user.setIsDeleted(0);
        user.setBanReason("临时封禁");
        user.setBannedBy(1L);
        user.setBannedTime(LocalDateTime.now().minusDays(2));
        user.setBannedUntil(LocalDateTime.now().minusMinutes(1));
        when(sysUserService.getById(123L)).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(123L, authentication.getPrincipal());
        assertEquals(1, user.getStatus());
        assertNull(user.getBanReason());
        assertNull(user.getBannedUntil());
        verify(sysUserService).updateById(user);
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldIgnoreQueryToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, jwtProperties, sysUserService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resume/download-pdf/20260520010203004");
        request.setParameter("token", "query-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtProperties.getHeader()).thenReturn("Authorization");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(sysUserService);
    }
}
