package com.airesume.server.service.impl;

import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.mapper.SysPromptMapper;
import com.airesume.server.service.SysPromptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public SysPrompt getActivePromptByJobRole(Integer scenarioType, String jobRoleCode, Integer difficulty) {
        if (scenarioType == null || jobRoleCode == null || difficulty == null) {
            return null;
        }
        SysPrompt prompt = this.getOne(
                Wrappers.<SysPrompt>lambdaQuery()
                        .eq(SysPrompt::getScenarioType, scenarioType)
                        .eq(SysPrompt::getJobRoleCode, jobRoleCode)
                        .eq(SysPrompt::getDifficulty, difficulty)
                        .eq(SysPrompt::getIsActive, STATUS_ACTIVE)
                        .last("LIMIT 1"),
                false
        );
        if (prompt == null) {
            log.debug("未找到启用的Prompt模板, scenarioType: {}, jobRoleCode: {}, difficulty: {}",
                    scenarioType, jobRoleCode, difficulty);
            return null;
        }
        log.debug("找到启用的Prompt模板, id: {}, scenarioType: {}, jobRoleCode: {}, difficulty: {}",
                prompt.getId(), scenarioType, jobRoleCode, difficulty);
        return prompt;
    }

    @Override
    public void deactivateOtherPrompts(Integer scenarioType, String jobRoleCode, Integer difficulty) {
        if (scenarioType == null || jobRoleCode == null || difficulty == null) {
            return;
        }
        LambdaQueryWrapper<SysPrompt> wrapper = Wrappers.<SysPrompt>lambdaQuery()
                .eq(SysPrompt::getScenarioType, scenarioType)
                .eq(SysPrompt::getJobRoleCode, jobRoleCode)
                .eq(SysPrompt::getDifficulty, difficulty)
                .eq(SysPrompt::getIsActive, STATUS_ACTIVE);
        List<SysPrompt> activePrompts = this.list(wrapper);
        if (activePrompts.isEmpty()) {
            return;
        }
        for (SysPrompt p : activePrompts) {
            p.setIsActive(PromptConstants.INACTIVE);
        }
        this.updateBatchById(activePrompts);
        log.info("已禁用同一岗位+难度的其他Prompt, scenarioType: {}, jobRoleCode: {}, difficulty: {}, count: {}",
                scenarioType, jobRoleCode, difficulty, activePrompts.size());
    }
}
