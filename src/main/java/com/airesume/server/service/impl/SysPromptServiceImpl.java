package com.airesume.server.service.impl;

import com.airesume.server.entity.SysPrompt;
import com.airesume.server.mapper.SysPromptMapper;
import com.airesume.server.service.SysPromptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI提示词模板服务实现类
 */
@Slf4j
@Service
public class SysPromptServiceImpl extends ServiceImpl<SysPromptMapper, SysPrompt> implements SysPromptService {
}
