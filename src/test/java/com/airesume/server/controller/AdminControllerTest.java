package com.airesume.server.controller;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminUserBanRequest;
import com.airesume.server.dto.admin.AdminUserUnbanRequest;
import com.airesume.server.dto.admin.BatchActiveRequest;
import com.airesume.server.dto.admin.BatchUserBanRequest;
import com.airesume.server.dto.admin.UserQuotaResponse;
import com.airesume.server.dto.admin.UserQuotaUpdateRequest;
import com.airesume.server.dto.admin.UserRightsUpdateRequest;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.service.AdminDashboardService;
import com.airesume.server.service.AdminUserRightsService;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.airesume.server.service.AiModelDiscoveryService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminDashboardService adminDashboardService;
    @Mock private AdminUserRightsService adminUserRightsService;
    @Mock private SysAiEngineConfigService sysAiEngineConfigService;
    @Mock private AiEngineConnectivityTestService aiEngineConnectivityTestService;
    @Mock private AiModelDiscoveryService aiModelDiscoveryService;
    @Mock private SysPromptService sysPromptService;
    @Mock private SysJobRoleService sysJobRoleService;
    @Mock private SysConfigService sysConfigService;
    @Mock private SysUserService sysUserService;
    @Mock private UserQuotaService userQuotaService;
    @Mock private AiCredentialCrypto aiCredentialCrypto;
    @Mock private NotificationService notificationService;
    @Mock private Authentication authentication;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysUser.class);
        controller = new AdminController(
                adminDashboardService,
                adminUserRightsService,
                sysAiEngineConfigService,
                aiEngineConnectivityTestService,
                aiModelDiscoveryService,
                sysPromptService,
                sysJobRoleService,
                sysConfigService,
                sysUserService,
                userQuotaService,
                aiCredentialCrypto,
                notificationService
        );

        lenient().when(authentication.getPrincipal()).thenReturn(1L);
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole(UserRoleConstants.ROLE_ADMIN);
        lenient().when(sysUserService.getById(1L)).thenReturn(admin);
    }

    @Test
    void getUserListShouldNormalizePageSizeAndFilterVipStateInServerQuery() {
        SysUser vipUser = new SysUser();
        vipUser.setId(10L);
        vipUser.setUsername("vip-user");
        vipUser.setRole(UserRoleConstants.ROLE_VIP);
        vipUser.setStatus(1);
        vipUser.setVipExpireTime(LocalDateTime.now().plusDays(1));

        Page<SysUser> pageResult = new Page<>(1, 10000, 1);
        pageResult.setRecords(List.of(vipUser));
        when(sysUserService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getUserList(
                0, 20000, "vip", null, null, "active", authentication);

        ArgumentCaptor<Page<SysUser>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper<SysUser>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(sysUserService).page(pageCaptor.capture(), wrapperCaptor.capture());

        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(10000L, pageCaptor.getValue().getSize());
        assertEquals(1, result.getData().get("total"));

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("role"));
        assertTrue(sqlSegment.contains("vip_expire_time"));
    }

    @Test
    void getUserQuotaShouldUseLoadedQuotaAndRefreshOnlyOnce() {
        SysUser target = new SysUser();
        target.setId(2L);
        target.setUsername("target-user");
        when(sysUserService.getById(2L)).thenReturn(target);

        UserQuota quota = new UserQuota();
        quota.setId(10L);
        quota.setUserId(2L);
        quota.setInterviewQuota(4);
        quota.setResumeQuota(5);
        quota.setTotalInterviewUsed(1);
        quota.setTotalResumeUsed(2);
        quota.setDailyInterviewUsed(3);
        quota.setDailyResumeUsed(4);
        when(userQuotaService.getByUserId(2L)).thenReturn(quota);

        Result<UserQuotaResponse> result = controller.getUserQuota(2L, authentication);

        assertEquals(4, result.getData().getInterviewQuota());
        assertEquals(5, result.getData().getResumeQuota());
        assertEquals("target-user", result.getData().getUsername());
        verify(userQuotaService).getByUserId(2L);
        verify(userQuotaService).refreshDailyQuotaIfNeeded(2L, quota);
        verify(userQuotaService, never()).getRemainingInterviewQuota(2L);
        verify(userQuotaService, never()).getRemainingResumeQuota(2L);
    }

    @Test
    void getUserStatsShouldUseAdminUserStatsCache() throws NoSuchMethodException {
        Cacheable cacheable = AdminController.class
                .getMethod("getUserStats", Authentication.class)
                .getAnnotation(Cacheable.class);

        assertNotNull(cacheable);
        assertEquals("admin:userStats", cacheable.value()[0]);
        assertEquals("'overview'", cacheable.key());
    }

    @Test
    void userMutationEndpointsShouldEvictAdminUserStatsCache() throws NoSuchMethodException {
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "updateUserRights", Long.class, UserRightsUpdateRequest.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "updateUserStatus", Long.class, Integer.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "updateUsersBatchStatus", BatchActiveRequest.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "banUser", Long.class, AdminUserBanRequest.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "unbanUser", Long.class, AdminUserUnbanRequest.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "banUsersBatch", BatchUserBanRequest.class, Authentication.class));
        assertEvictsAdminUserStats(AdminController.class.getMethod(
                "updateUserQuota", UserQuotaUpdateRequest.class, Authentication.class));
    }

    @Test
    void banUserShouldSetStatusAndBanMetadata() {
        SysUser target = new SysUser();
        target.setId(2L);
        target.setUsername("target");
        target.setRole(UserRoleConstants.ROLE_NORMAL);
        target.setStatus(1);
        when(sysUserService.getById(2L)).thenReturn(target);

        AdminUserBanRequest request = new AdminUserBanRequest();
        request.setDuration("7d");
        request.setReason("连续发布违规内容");

        controller.banUser(2L, request, authentication);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals(0, updated.getStatus());
        assertEquals("连续发布违规内容", updated.getBanReason());
        assertEquals(1L, updated.getBannedBy());
        assertNotNull(updated.getBannedTime());
        assertNotNull(updated.getBannedUntil());
        verify(notificationService).createNotification(
                eq(2L),
                eq("system"),
                eq("账号已被封禁"),
                contains("连续发布违规内容"),
                eq("user_ban"),
                eq("2")
        );
    }

    @Test
    void banUserShouldRejectSelfBan() {
        AdminUserBanRequest request = new AdminUserBanRequest();
        request.setDuration("1d");
        request.setReason("测试");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.banUser(1L, request, authentication));

        assertEquals("不能封禁当前管理员账号", exception.getMessage());
        verify(sysUserService, never()).updateById(any(SysUser.class));
    }

    @Test
    void banUserShouldRejectAdminTarget() {
        SysUser target = new SysUser();
        target.setId(9L);
        target.setUsername("other-admin");
        target.setRole(UserRoleConstants.ROLE_ADMIN);
        target.setStatus(1);
        when(sysUserService.getById(9L)).thenReturn(target);

        AdminUserBanRequest request = new AdminUserBanRequest();
        request.setDuration("1d");
        request.setReason("测试");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.banUser(9L, request, authentication));

        assertEquals("不能封禁管理员账号", exception.getMessage());
        verify(sysUserService, never()).updateById(any(SysUser.class));
    }

    @Test
    void unbanUserShouldClearBanMetadata() {
        SysUser target = new SysUser();
        target.setId(2L);
        target.setUsername("target");
        target.setRole(UserRoleConstants.ROLE_NORMAL);
        target.setStatus(0);
        target.setBanReason("违规");
        target.setBannedBy(1L);
        target.setBannedTime(LocalDateTime.now());
        target.setBannedUntil(LocalDateTime.now().plusDays(1));
        when(sysUserService.getById(2L)).thenReturn(target);

        AdminUserUnbanRequest request = new AdminUserUnbanRequest();
        request.setReason("申诉通过");

        controller.unbanUser(2L, request, authentication);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals(1, updated.getStatus());
        assertNull(updated.getBanReason());
        assertNull(updated.getBannedBy());
        assertNull(updated.getBannedTime());
        assertNull(updated.getBannedUntil());
        verify(notificationService).createNotification(
                eq(2L),
                eq("system"),
                eq("账号已解封"),
                contains("申诉通过"),
                eq("user_unban"),
                eq("2")
        );
    }

    @Test
    void updateUserQuotaShouldEvictUserQuotaCacheForTargetUser() throws NoSuchMethodException {
        Method method = AdminController.class.getMethod(
                "updateUserQuota", UserQuotaUpdateRequest.class, Authentication.class);

        CacheEvict[] evicts = collectCacheEvicts(method);
        boolean found = Arrays.stream(evicts)
                .anyMatch(evict -> Arrays.asList(evict.value()).contains("user:quota")
                        && "#request.userId".equals(evict.key()));

        assertTrue(found, "updateUserQuota should evict user:quota for the adjusted user");
    }

    private void assertEvictsAdminUserStats(Method method) {
        CacheEvict[] evicts = collectCacheEvicts(method);
        boolean found = Arrays.stream(evicts)
                .anyMatch(evict -> Arrays.asList(evict.value()).contains("admin:userStats") && evict.allEntries());
        assertTrue(found, method.getName() + " should evict admin:userStats");
    }

    private CacheEvict[] collectCacheEvicts(Method method) {
        Caching caching = method.getAnnotation(Caching.class);
        if (caching != null) {
            return caching.evict();
        }
        CacheEvict evict = method.getAnnotation(CacheEvict.class);
        return evict == null ? new CacheEvict[0] : new CacheEvict[] { evict };
    }
}
