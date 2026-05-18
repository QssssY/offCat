package com.airesume.server.service.impl;

import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.mapper.SysGrowthConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysGrowthConfigServiceImplTest {

    @Mock
    private SysGrowthConfigMapper sysGrowthConfigMapper;

    @Test
    void getByGroupShouldReturnConfigsInGroup() {
        SysGrowthConfigServiceImpl service = new SysGrowthConfigServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysGrowthConfigMapper);

        SysGrowthConfig config1 = new SysGrowthConfig();
        config1.setId(1L);
        config1.setConfigKey("key1");
        config1.setConfigValue("val1");
        config1.setGroupName("achievement");
        config1.setSort(1);

        when(sysGrowthConfigMapper.selectList(any())).thenReturn(List.of(config1));

        List<SysGrowthConfig> result = service.getByGroup("achievement");
        assertEquals(1, result.size());
        assertEquals("key1", result.get(0).getConfigKey());
    }

    @Test
    void getByGroupShouldReturnEmptyForUnknownGroup() {
        SysGrowthConfigServiceImpl service = new SysGrowthConfigServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysGrowthConfigMapper);

        when(sysGrowthConfigMapper.selectList(any())).thenReturn(List.of());

        List<SysGrowthConfig> result = service.getByGroup("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void getValueShouldReturnConfigValue() {
        SysGrowthConfigServiceImpl service = new SysGrowthConfigServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysGrowthConfigMapper);

        SysGrowthConfig config = new SysGrowthConfig();
        config.setConfigKey("test_key");
        config.setConfigValue("test_value");

        when(sysGrowthConfigMapper.selectOne(any(), anyBoolean())).thenReturn(config);

        String result = service.getValue("test_key");
        assertEquals("test_value", result);
    }

    @Test
    void getValueShouldReturnNullWhenNotFound() {
        SysGrowthConfigServiceImpl service = new SysGrowthConfigServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysGrowthConfigMapper);

        when(sysGrowthConfigMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        String result = service.getValue("nonexistent_key");
        assertNull(result);
    }
}
