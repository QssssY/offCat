package com.airesume.server.service.impl;

import com.airesume.server.entity.SysPrompt;
import com.airesume.server.mapper.SysPromptMapper;
import com.airesume.server.service.SysPromptService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysPromptServiceImpl extends ServiceImpl<SysPromptMapper, SysPrompt> implements SysPromptService {

    private static final Integer STATUS_ACTIVE = 1;

    @Override
    public String getActivePromptContent(Integer scenarioType) {
        if (scenarioType == null) {
            return null;
        }
        SysPrompt prompt = this.getOne(
                Wrappers.<SysPrompt>lambdaQuery()
                        .eq(SysPrompt::getScenarioType, scenarioType)
                        .eq(SysPrompt::getIsActive, STATUS_ACTIVE)
                        .last("LIMIT 1"),
                false
        );
        if (prompt == null) {
            log.warn("未找到启用的Prompt模板, scenarioType: {}", scenarioType);
            return null;
        }
        log.debug("找到启用的Prompt模板, id: {}, scenarioType: {}", prompt.getId(), scenarioType);
        return prompt.getPromptContent();
    }
}
