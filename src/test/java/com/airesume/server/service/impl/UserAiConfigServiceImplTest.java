package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.mapper.UserAiConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAiConfigServiceImplTest {

    private UserAiConfigMapper mapper;
    private UserAiConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserAiConfig.class);
        mapper = mock(UserAiConfigMapper.class);
        service = new UserAiConfigServiceImpl(
                mock(AiCredentialCrypto.class),
                mock(AiEngineConnectivityTestService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void shouldPhysicallyDeleteActiveUserAiConfig() {
        when(mapper.deleteActiveConfig(7L, "resume")).thenReturn(1);

        service.deleteUserConfig(7L, "resume");

        verify(mapper).deleteActiveConfig(7L, "resume");
    }

    @Test
    void shouldRejectDeletingMissingUserAiConfig() {
        when(mapper.deleteActiveConfig(7L, "resume")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.deleteUserConfig(7L, "resume"));
    }
}
