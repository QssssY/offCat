package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.dto.admin.AiEngineConnectivityTestRequest;
import com.airesume.server.dto.admin.AiEngineConnectivityTestResponse;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.AdminDashboardService;
import com.airesume.server.service.AdminUserRightsService;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAiEngineConnectivityControllerTest {

    @Test
    void shouldUseStoredApiKeyWhenEditRequestApiKeyIsBlank() {
        SysAiEngineConfigService configService = mock(SysAiEngineConfigService.class);
        AiCredentialCrypto credentialCrypto = mock(AiCredentialCrypto.class);
        AiEngineConnectivityTestService connectivityTestService = mock(AiEngineConnectivityTestService.class);
        SysUserService sysUserService = mock(SysUserService.class);
        AdminController controller = buildController(configService, credentialCrypto, connectivityTestService, sysUserService);
        Authentication authentication = mockAdminAuthentication(sysUserService);
        SysAiEngineConfig config = new SysAiEngineConfig();
        config.setApiKey("encrypted-key");
        when(configService.getById(10L)).thenReturn(config);
        when(credentialCrypto.decrypt("encrypted-key")).thenReturn("stored-key");
        AiEngineConnectivityTestRequest request = buildRequest();
        request.setId(10L);
        request.setApiKey("");
        AiEngineConnectivityTestResponse response = AiEngineConnectivityTestResponse.builder()
                .success(true)
                .message("连通测试成功")
                .build();
        when(connectivityTestService.testConnectivity(request, "stored-key")).thenReturn(response);

        Result<AiEngineConnectivityTestResponse> result =
                controller.testAiEngineConnectivity(request, authentication);

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        verify(connectivityTestService).testConnectivity(request, "stored-key");
    }

    @Test
    void shouldRejectCreateConnectivityTestWithoutApiKey() {
        SysAiEngineConfigService configService = mock(SysAiEngineConfigService.class);
        AiCredentialCrypto credentialCrypto = mock(AiCredentialCrypto.class);
        AiEngineConnectivityTestService connectivityTestService = mock(AiEngineConnectivityTestService.class);
        SysUserService sysUserService = mock(SysUserService.class);
        AdminController controller = buildController(configService, credentialCrypto, connectivityTestService, sysUserService);
        Authentication authentication = mockAdminAuthentication(sysUserService);

        AiEngineConnectivityTestRequest request = buildRequest();
        request.setApiKey("");

        assertThrows(BusinessException.class, () -> controller.testAiEngineConnectivity(request, authentication));
    }

    @Test
    void shouldSkipApiKeyLookupWhenTestingMockProvider() {
        SysAiEngineConfigService configService = mock(SysAiEngineConfigService.class);
        AiCredentialCrypto credentialCrypto = mock(AiCredentialCrypto.class);
        AiEngineConnectivityTestService connectivityTestService = mock(AiEngineConnectivityTestService.class);
        SysUserService sysUserService = mock(SysUserService.class);
        AdminController controller = buildController(configService, credentialCrypto, connectivityTestService, sysUserService);
        Authentication authentication = mockAdminAuthentication(sysUserService);
        AiEngineConnectivityTestRequest request = buildRequest();
        request.setProviderType("mock");
        request.setApiKey("");
        AiEngineConnectivityTestResponse response = AiEngineConnectivityTestResponse.builder()
                .success(true)
                .message("Mock 引擎无需外部网络连通，配置格式有效。")
                .build();
        when(connectivityTestService.testConnectivity(request, null)).thenReturn(response);

        Result<AiEngineConnectivityTestResponse> result =
                controller.testAiEngineConnectivity(request, authentication);

        assertEquals(200, result.getCode());
        assertEquals(response, result.getData());
        verify(connectivityTestService).testConnectivity(request, null);
    }

    private AdminController buildController(SysAiEngineConfigService configService,
                                            AiCredentialCrypto credentialCrypto,
                                            AiEngineConnectivityTestService connectivityTestService,
                                            SysUserService sysUserService) {
        return new AdminController(
                mock(AdminDashboardService.class),
                mock(AdminUserRightsService.class),
                configService,
                connectivityTestService,
                mock(SysPromptService.class),
                mock(SysJobRoleService.class),
                sysUserService,
                mock(UserQuotaService.class),
                credentialCrypto
        );
    }

    private Authentication mockAdminAuthentication(SysUserService sysUserService) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(1L);
        SysUser admin = new SysUser();
        admin.setRole(UserRoleConstants.ROLE_ADMIN);
        when(sysUserService.getById(1L)).thenReturn(admin);
        return authentication;
    }

    private AiEngineConnectivityTestRequest buildRequest() {
        AiEngineConnectivityTestRequest request = new AiEngineConnectivityTestRequest();
        request.setProviderType("openai");
        request.setModelName("gpt-test");
        request.setBaseUrl("https://api.example.com/v1");
        request.setApiKey("sk-real");
        request.setTimeoutMs(30000);
        return request;
    }
}
