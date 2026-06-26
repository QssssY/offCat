package com.airesume.server.service.impl;

import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.mapper.SysJobRoleMapper;
import com.airesume.server.mapper.SysPromptMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SysPromptServiceImplTest {

    @Mock
    private SysPromptMapper sysPromptMapper;

    @Mock
    private SysJobRoleMapper sysJobRoleMapper;

    @Test
    void shouldLockJobRoleBeforeDeactivatingPromptGroup() {
        SysPromptServiceImpl service = spy(new SysPromptServiceImpl(sysJobRoleMapper));
        ReflectionTestUtils.setField(service, "baseMapper", sysPromptMapper);

        List<SysPrompt> activePrompts = new ArrayList<>(List.of(createActivePrompt(), createActivePrompt()));
        doReturn(activePrompts).when(service).list(any(Wrapper.class));
        doReturn(true).when(service).updateBatchById(anyCollection());

        service.deactivateOtherPrompts(1, "java-dev", 2);

        InOrder inOrder = inOrder(sysJobRoleMapper, service);
        inOrder.verify(sysJobRoleMapper).lockIdByRoleCode("java-dev");
        inOrder.verify(service).list(any(Wrapper.class));
        inOrder.verify(service).updateBatchById(activePrompts);
        activePrompts.forEach(prompt -> assertEquals(PromptConstants.INACTIVE, prompt.getIsActive()));
    }

    @Test
    void shouldSkipPromptDeactivationWhenRequiredParamsAreMissing() {
        SysPromptServiceImpl service = spy(new SysPromptServiceImpl(sysJobRoleMapper));
        ReflectionTestUtils.setField(service, "baseMapper", sysPromptMapper);

        service.deactivateOtherPrompts(null, "java-dev", 2);

        verify(service, never()).list(any(Wrapper.class));
        verify(service, never()).updateBatchById(anyCollection());
        verifyNoInteractions(sysJobRoleMapper);
    }

    private SysPrompt createActivePrompt() {
        SysPrompt prompt = new SysPrompt();
        prompt.setIsActive(PromptConstants.ACTIVE);
        return prompt;
    }
}
