package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.GrowthConfigCreateRequest;
import com.airesume.server.dto.admin.GrowthConfigResponse;
import com.airesume.server.dto.admin.GrowthConfigUpdateRequest;
import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.service.SysGrowthConfigService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGrowthConfigControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private SysGrowthConfigService sysGrowthConfigService;
    @Mock private Authentication authentication;

    private AdminGrowthConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminGrowthConfigController(sysGrowthConfigService);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getConfigListShouldReturnPagedRecordsWhenNoGroup() {
        SysGrowthConfig config = buildConfig("daily_login_reward", "achievement");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysGrowthConfig> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        Page<SysGrowthConfig> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(config));
        when(sysGrowthConfigService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).orderByAsc(any(SFunction.class));
        doReturn(pageResult).when(wrapper).page(any(Page.class));

        Result<Map<String, Object>> result = controller.getConfigList(null, 1, 20, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<GrowthConfigResponse> records = (List<GrowthConfigResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("daily_login_reward", records.get(0).getConfigKey());
        assertEquals(1, result.getData().get("total"));
    }

    @Test
    void getConfigListShouldReturnPagedRecordsByGroup() {
        SysGrowthConfig config = buildConfig("milestone_first_interview", "milestone");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysGrowthConfig> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        Page<SysGrowthConfig> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(config));
        when(sysGrowthConfigService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        doReturn(wrapper).when(wrapper).orderByAsc(any(SFunction.class));
        doReturn(pageResult).when(wrapper).page(any(Page.class));

        Result<Map<String, Object>> result = controller.getConfigList("milestone", 1, 20, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<GrowthConfigResponse> records = (List<GrowthConfigResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("milestone_first_interview", records.get(0).getConfigKey());
    }

    @Test
    void createConfigShouldReturnNewId() {
        GrowthConfigCreateRequest request = new GrowthConfigCreateRequest();
        request.setConfigKey("new_config");
        request.setConfigValue("value");
        request.setGroupName("default");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysGrowthConfig> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(sysGrowthConfigService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(0L);
        when(sysGrowthConfigService.save(any(SysGrowthConfig.class))).thenAnswer(invocation -> {
            SysGrowthConfig saved = invocation.getArgument(0);
            saved.setId(200L);
            return true;
        });

        Result<Long> result = controller.createConfig(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(200L, result.getData());
    }

    @Test
    void createConfigShouldThrowWhenKeyExists() {
        GrowthConfigCreateRequest request = new GrowthConfigCreateRequest();
        request.setConfigKey("existing_key");
        request.setConfigValue("value");

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<SysGrowthConfig> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(sysGrowthConfigService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.createConfig(request, authentication));
    }

    @Test
    void updateConfigShouldSucceed() {
        GrowthConfigUpdateRequest request = new GrowthConfigUpdateRequest();
        request.setId(100L);
        request.setConfigValue("new_value");

        SysGrowthConfig existingConfig = buildConfig("test_key", "default");
        existingConfig.setId(100L);
        when(sysGrowthConfigService.getById(100L)).thenReturn(existingConfig);

        Result<Void> result = controller.updateConfig(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysGrowthConfigService).updateById(existingConfig);
        assertEquals("new_value", existingConfig.getConfigValue());
    }

    @Test
    void updateConfigShouldThrowWhenNotFound() {
        GrowthConfigUpdateRequest request = new GrowthConfigUpdateRequest();
        request.setId(999L);
        when(sysGrowthConfigService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.updateConfig(request, authentication));
    }

    @Test
    void deleteConfigShouldRemove() {
        Result<Void> result = controller.deleteConfig(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysGrowthConfigService).removeById(100L);
    }

    @Test
    void deleteConfigsBatchShouldRemoveIds() {
        Result<Void> result = controller.deleteConfigsBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(sysGrowthConfigService).removeByIds(List.of(100L, 200L));
    }

    @Test
    void deleteConfigsBatchShouldRejectEmptyIds() {
        assertThrows(BusinessException.class, () -> controller.deleteConfigsBatch(List.of(), authentication));
    }

    private SysGrowthConfig buildConfig(String key, String groupName) {
        SysGrowthConfig config = new SysGrowthConfig();
        config.setId(100L);
        config.setConfigKey(key);
        config.setConfigValue("5");
        config.setDescription("description");
        config.setGroupName(groupName);
        config.setSort(1);
        config.setCreateTime(LocalDateTime.now());
        return config;
    }
}
