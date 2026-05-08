package com.airesume.server.controller;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.*;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.entity.SysJobRole;
import org.springframework.cache.annotation.CacheEvict;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.service.AdminDashboardService;
import com.airesume.server.service.AdminUserRightsService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 管理端控制器
 * 提供提示词模板管理、用户管理、额度管理等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserRightsService adminUserRightsService;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final SysPromptService sysPromptService;
    private final SysJobRoleService sysJobRoleService;
    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;

    // ==================== 提示词模板管理接口 ====================

    // ==================== 岗位配置接口 ====================

    /**
     * 查询岗位配置列表
     *
     * 作用：
     * 管理员通过这个接口查看全部岗位配置，包括已禁用岗位。
     * 用户端读取岗位选项时只会拿启用数据，因此管理端需要保留完整视图。
     */
    @GetMapping("/job-roles")
    public Result<List<JobRoleResponse>> getJobRoleList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get job role list");

        List<JobRoleResponse> responses = sysJobRoleService.listAllOrdered().stream()
                .map(this::buildJobRoleResponse)
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    /**
     * 新增岗位配置
     */
    @PostMapping("/job-roles")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Long> createJobRole(@Valid @RequestBody JobRoleCreateRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin create job role, roleCode: {}, roleName: {}", request.getRoleCode(), request.getRoleName());

        validateJobRoleUniqueness(request.getRoleCode(), request.getRoleName(), null);

        SysJobRole jobRole = new SysJobRole();
        jobRole.setRoleCode(request.getRoleCode().trim());
        jobRole.setRoleName(request.getRoleName().trim());
        jobRole.setInterviewTag(trimToNull(request.getInterviewTag()));
        jobRole.setTagType(trimToNull(request.getTagType()));
        jobRole.setIsActive(1);
        jobRole.setSort(request.getSort());
        sysJobRoleService.save(jobRole);

        return Result.success("岗位创建成功", jobRole.getId());
    }

    /**
     * 修改岗位配置
     */
    @PutMapping("/job-roles")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Void> updateJobRole(@Valid @RequestBody JobRoleUpdateRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin update job role, id: {}", request.getId());

        SysJobRole jobRole = sysJobRoleService.getById(request.getId());
        if (jobRole == null) {
            throw new BusinessException("岗位配置不存在");
        }

        String nextRoleCode = request.getRoleCode() != null ? request.getRoleCode().trim() : jobRole.getRoleCode();
        String nextRoleName = request.getRoleName() != null ? request.getRoleName().trim() : jobRole.getRoleName();
        validateJobRoleUniqueness(nextRoleCode, nextRoleName, jobRole.getId());

        if (request.getRoleCode() != null) {
            jobRole.setRoleCode(nextRoleCode);
        }
        if (request.getRoleName() != null) {
            jobRole.setRoleName(nextRoleName);
        }
        if (request.getInterviewTag() != null) {
            jobRole.setInterviewTag(trimToNull(request.getInterviewTag()));
        }
        if (request.getTagType() != null) {
            jobRole.setTagType(trimToNull(request.getTagType()));
        }
        if (request.getIsActive() != null) {
            jobRole.setIsActive(request.getIsActive());
        }
        if (request.getSort() != null) {
            jobRole.setSort(request.getSort());
        }

        sysJobRoleService.updateById(jobRole);
        return Result.success("岗位更新成功", null);
    }

    /**
     * 启用/禁用岗位配置
     */
    @PutMapping("/job-roles/{id}/active")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Void> toggleJobRoleActive(@PathVariable Long id,
                                            @RequestParam Integer isActive,
                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin toggle job role active, id: {}, isActive: {}", id, isActive);
        if (isActive != 0 && isActive != 1) {
            throw new BusinessException("isActive 只能为 0 或 1");
        }

        SysJobRole jobRole = sysJobRoleService.getById(id);
        if (jobRole == null) {
            throw new BusinessException("岗位配置不存在");
        }

        jobRole.setIsActive(isActive);
        sysJobRoleService.updateById(jobRole);
        return Result.success("岗位状态更新成功", null);
    }

    /**
     * 删除岗位配置（物理删除）
     *
     * 作用：
     * 管理员可以通过此接口物理删除岗位配置，删除后数据无法恢复。
     * 此操作会绕过逻辑删除，直接从数据库移除记录。
     */
    @DeleteMapping("/job-roles/{id}")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Void> deleteJobRole(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin delete job role, id: {}", id);

        SysJobRole jobRole = sysJobRoleService.getById(id);
        if (jobRole == null) {
            throw new BusinessException("岗位配置不存在");
        }

        // 物理删除：直接使用 removeById 绕过逻辑删除机制
        sysJobRoleService.removeById(id);
        return Result.success("岗位删除成功", null);
    }

    /**
     * 批量删除岗位配置（物理删除）
     *
     * 作用：
     * 管理员可以批量物理删除岗位配置，删除后数据无法恢复。
     */
    @DeleteMapping("/job-roles/batch")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Void> deleteJobRolesBatch(@RequestBody List<Long> ids, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch delete job roles, ids: {}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的岗位配置");
        }

        // 物理删除：批量移除
        sysJobRoleService.removeByIds(ids, false);
        log.info("Batch delete job roles completed, count: {}", ids.size());
        return Result.success("批量删除成功", null);
    }

    /**
     * 批量启用/禁用岗位配置
     *
     * 作用：
     * 管理员可以批量启用或禁用岗位配置。
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/job-roles/batch/active")
    @CacheEvict(value = "config:jobRoles", allEntries = true)
    public Result<Void> toggleJobRolesBatchActive(@Valid @RequestBody BatchActiveRequest request,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch toggle job roles active, ids: {}, isActive: {}", request.getIds(), request.getIsActive());

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("请选择要操作的岗位配置");
        }

        for (Long id : request.getIds()) {
            SysJobRole jobRole = sysJobRoleService.getById(id);
            if (jobRole != null) {
                jobRole.setIsActive(request.getIsActive());
                sysJobRoleService.updateById(jobRole);
            }
        }
        log.info("Batch toggle job roles active completed, count: {}", request.getIds().size());
        return Result.success("批量更新成功", null);
    }

    /**
     * 查询提示词模板列表
     *
     * @param authentication 认证对象
     * @return 提示词模板列表
     */
    /**
     * 查询 AI 引擎配置列表
     *
     * 作用：
     * 管理端通过这个接口统一查看 interview / resume 两类业务的模型配置。
     * 列表中的 apiKey 必须脱敏，避免后台接口把敏感信息原样暴露到前端。
     */
    @GetMapping("/ai-engines")
    public Result<List<AiEngineConfigResponse>> getAiEngineConfigList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get AI engine config list");

        List<AiEngineConfigResponse> responses = sysAiEngineConfigService.listAllOrdered().stream()
                .map(this::buildAiEngineConfigResponse)
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    /**
     * 新增 AI 引擎配置
     */
    @PostMapping("/ai-engines")
    public Result<Long> createAiEngineConfig(@Valid @RequestBody AiEngineConfigCreateRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);

        String engineCode = normalizeRequiredValue(request.getEngineCode(), "引擎编码不能为空");
        validateAiEngineCodeUniqueness(engineCode, null);

        String businessType = normalizeBusinessType(request.getBusinessType());
        sysAiEngineConfigService.validateBusinessType(businessType);

        log.info("Admin create AI engine config, engineCode: {}, businessType: {}", engineCode, businessType);

        SysAiEngineConfig config = new SysAiEngineConfig();
        config.setEngineCode(engineCode);
        config.setEngineName(normalizeRequiredValue(request.getEngineName(), "引擎名称不能为空"));
        config.setProviderType(normalizeRequiredValue(request.getProviderType(), "提供商类型不能为空"));
        config.setBusinessType(businessType);
        config.setModelName(normalizeRequiredValue(request.getModelName(), "模型名称不能为空"));
        config.setBaseUrl(normalizeRequiredValue(request.getBaseUrl(), "基础地址不能为空"));
        config.setApiKey(normalizeRequiredValue(request.getApiKey(), "API Key 不能为空"));
        config.setSupportsMultimodal(request.getSupportsMultimodal());
        config.setThinkingMode(request.getThinkingMode());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        config.setTimeoutMs(request.getTimeoutMs());
        config.setIsActive(request.getIsActive());
        config.setSort(request.getSort());
        config.setRemark(trimToNull(request.getRemark()));

        sysAiEngineConfigService.saveConfig(config);
        return Result.success("AI 引擎配置创建成功", config.getId());
    }

    /**
     * 修改 AI 引擎配置
     */
    @PutMapping("/ai-engines")
    public Result<Void> updateAiEngineConfig(@Valid @RequestBody AiEngineConfigUpdateRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin update AI engine config, id: {}", request.getId());

        SysAiEngineConfig config = sysAiEngineConfigService.getById(request.getId());
        if (config == null) {
            throw new BusinessException("AI 引擎配置不存在");
        }

        String nextEngineCode = request.getEngineCode() != null
                ? normalizeRequiredValue(request.getEngineCode(), "引擎编码不能为空")
                : config.getEngineCode();
        validateAiEngineCodeUniqueness(nextEngineCode, config.getId());

        if (request.getEngineCode() != null) {
            config.setEngineCode(nextEngineCode);
        }
        if (request.getEngineName() != null) {
            config.setEngineName(normalizeRequiredValue(request.getEngineName(), "引擎名称不能为空"));
        }
        if (request.getProviderType() != null) {
            config.setProviderType(normalizeRequiredValue(request.getProviderType(), "提供商类型不能为空"));
        }
        if (request.getBusinessType() != null) {
            String businessType = normalizeBusinessType(request.getBusinessType());
            sysAiEngineConfigService.validateBusinessType(businessType);
            config.setBusinessType(businessType);
        }
        if (request.getModelName() != null) {
            config.setModelName(normalizeRequiredValue(request.getModelName(), "模型名称不能为空"));
        }
        if (request.getBaseUrl() != null) {
            config.setBaseUrl(normalizeRequiredValue(request.getBaseUrl(), "基础地址不能为空"));
        }
        if (request.getApiKey() != null) {
            // 【关键修复】防止脱敏值误写入数据库
            // 如果提交的 apiKey 符合脱敏格式（如 "sk-****abcd"），则拒绝更新，防止前端把脱敏值覆盖真实值
            if (isMaskedApiKey(request.getApiKey())) {
                throw new BusinessException("API Key 不能为脱敏格式，请输入完整的真实 API Key");
            }
            config.setApiKey(normalizeRequiredValue(request.getApiKey(), "API Key 不能为空"));
        }
        if (request.getSupportsMultimodal() != null) {
            config.setSupportsMultimodal(request.getSupportsMultimodal());
        }
        if (request.getThinkingMode() != null) {
            config.setThinkingMode(request.getThinkingMode());
        }
        if (request.getTemperature() != null) {
            config.setTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            config.setMaxTokens(request.getMaxTokens());
        }
        if (request.getTimeoutMs() != null) {
            config.setTimeoutMs(request.getTimeoutMs());
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        if (request.getSort() != null) {
            config.setSort(request.getSort());
        }
        if (request.getRemark() != null) {
            config.setRemark(trimToNull(request.getRemark()));
        }

        sysAiEngineConfigService.updateConfig(config);
        return Result.success("AI 引擎配置更新成功", null);
    }

    /**
     * 启用 / 禁用 AI 引擎配置
     *
     * 作用：
     * 当前启用切换必须继续走后端接口，而不是前端自行处理状态。
     * 后端需要在这里保证同一 businessType 最多只有一个启用配置。
     */
    @PutMapping("/ai-engines/{id}/active")
    public Result<Void> toggleAiEngineConfigActive(@PathVariable Long id,
                                                   @RequestParam Integer isActive,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin toggle AI engine config active, id: {}, isActive: {}", id, isActive);

        sysAiEngineConfigService.switchActive(id, isActive);
        return Result.success("AI 引擎配置状态更新成功", null);
    }

    /**
     * 删除 AI 引擎配置（物理删除）
     *
     * 作用：
     * 管理员可以通过此接口物理删除 AI 引擎配置，删除后数据无法恢复。
     * 注意：如果删除的引擎正处于启用状态，需要谨慎操作。
     */
    @DeleteMapping("/ai-engines/{id}")
    public Result<Void> deleteAiEngine(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin delete AI engine, id: {}", id);

        SysAiEngineConfig config = sysAiEngineConfigService.getById(id);
        if (config == null) {
            throw new BusinessException("AI 引擎配置不存在");
        }

        // 物理删除：直接移除
        sysAiEngineConfigService.removeById(id);
        return Result.success("AI 引擎配置删除成功", null);
    }

    /**
     * 批量删除 AI 引擎配置（物理删除）
     *
     * 作用：
     * 管理员可以批量物理删除 AI 引擎配置，删除后数据无法恢复。
     */
    @DeleteMapping("/ai-engines/batch")
    public Result<Void> deleteAiEnginesBatch(@RequestBody List<Long> ids, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch delete AI engines, ids: {}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的AI引擎配置");
        }

        // 物理删除：批量移除
        sysAiEngineConfigService.removeByIds(ids, false);
        log.info("Batch delete AI engines completed, count: {}", ids.size());
        return Result.success("批量删除成功", null);
    }

/**
     * 批量启用/禁用 AI 引擎配置
     *
     * 作用：
     * 管理员可以批量启用或禁用 AI 引擎配置。
     * 启用时会自动禁用同业务类型的其他配置，保证最多只有一个启用。
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/ai-engines/batch/active")
    public Result<Void> toggleAiEnginesBatchActive(@Valid @RequestBody BatchActiveRequest request,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch toggle AI engines active, ids: {}, isActive: {}", request.getIds(), request.getIsActive());

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("请选择要操作的AI引擎配置");
        }

        // 使用事务确保同业务类型只有一个启用配置
        for (Long id : request.getIds()) {
            sysAiEngineConfigService.switchActive(id, request.getIsActive());
        }
        log.info("Batch toggle AI engines active completed, count: {}", request.getIds().size());
        return Result.success("批量更新成功", null);
    }

    /**
     * 管理端看板总览统计接口。
     *
     * 返回摘要卡片需要的高层统计数据，并支持日期范围筛选。
     */
    @GetMapping("/dashboard/overview")
    public Result<DashboardOverviewResponse> getDashboardOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get dashboard overview, startDate: {}, endDate: {}", startDate, endDate);
        return Result.success(adminDashboardService.getDashboardOverview(startDate, endDate));
    }

    /**
     * 管理端看板趋势统计接口。
     *
     * 支持参数化日期范围，且不依赖中间件监控指标。
     */
    @GetMapping("/dashboard/trends")
    public Result<List<DashboardTrendResponse>> getDashboardTrends(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get dashboard trends, startDate: {}, endDate: {}", startDate, endDate);
        return Result.success(adminDashboardService.getDashboardTrends(startDate, endDate));
    }

    /**
     * 管理端热门岗位排行接口，支持日期范围和 limit 参数。
     */
    @GetMapping("/dashboard/hot-job-roles")
    public Result<List<HotJobRoleResponse>> getHotJobRoles(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer limit,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get hot job role ranking, startDate: {}, endDate: {}, limit: {}", startDate, endDate, limit);
        return Result.success(adminDashboardService.getHotJobRoles(startDate, endDate, limit));
    }

    /**
     * 管理端业务分布统计接口。
     *
     * 返回所选日期范围内 interview/resume 的业务分布结果。
     */
    @GetMapping("/dashboard/business-distribution")
    public Result<BusinessDistributionResponse> getBusinessDistribution(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get business distribution, startDate: {}, endDate: {}", startDate, endDate);
        return Result.success(adminDashboardService.getBusinessDistribution(startDate, endDate));
    }

    /**
     * 管理端业务监控总览接口。
     *
     * 当前为应用层统计实现，直接读取业务表数据，
     * 即使 RabbitMQ/Redis 深度监控尚未接入也可使用。
     */
    @GetMapping("/monitor/overview")
    public Result<MonitorOverviewResponse> getMonitorOverview(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get monitor overview");
        return Result.success(adminDashboardService.getMonitorOverview());
    }

    @GetMapping("/prompts")
    public Result<List<PromptResponse>> getPromptList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get prompt list");

        List<SysPrompt> prompts = sysPromptService.list();
        List<PromptResponse> responses = prompts.stream()
                .map(this::buildPromptResponse)
                .collect(Collectors.toList());

        log.info("Prompt list fetched, count: {}", responses.size());
        return Result.success(responses);
    }

    /**
     * 新增提示词模板
     *
     * @param request 创建请求
     * @param authentication 认证对象
     * @return 新增的提示词模板 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/prompts")
    public Result<Long> createPrompt(@Valid @RequestBody PromptCreateRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        SysJobRole configuredJobRole = resolvePromptJobRole(request.getJobRoleCode(), request.getJobRole());
        log.info("Admin create prompt, scenarioType: {}, jobRoleCode: {}, jobRoleName: {}, difficulty: {}",
                request.getScenarioType(), configuredJobRole.getRoleCode(), configuredJobRole.getRoleName(), request.getDifficulty());

SysPrompt prompt = new SysPrompt();
        prompt.setScenarioType(request.getScenarioType());
        prompt.setJobRoleCode(configuredJobRole.getRoleCode());
        prompt.setJobRole(configuredJobRole.getRoleName());
        prompt.setDifficulty(request.getDifficulty());
        prompt.setPromptContent(request.getPromptContent());

        Integer isActive = request.getActiveStatus() != null ? request.getActiveStatus() : PromptConstants.ACTIVE;
        if (PromptConstants.ACTIVE ==(isActive)) {
            sysPromptService.deactivateOtherPrompts(
                    request.getScenarioType(),
                    configuredJobRole.getRoleCode(),
                    request.getDifficulty()
            );
        }
        prompt.setIsActive(isActive);
        sysPromptService.save(prompt);

        log.info("Prompt created, id: {}", prompt.getId());
        return Result.success("Prompt创建成功", prompt.getId());
    }

    /**
     * 修改提示词模板
     *
     * @param request 更新请求
     * @param authentication 认证对象
     * @return 空结果
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/prompts")
    public Result<Void> updatePrompt(@Valid @RequestBody PromptUpdateRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin update prompt, id: {}", request.getId());

        SysPrompt prompt = sysPromptService.getById(request.getId());
        if (prompt == null) {
            throw new BusinessException("Prompt不存在");
        }

        if (request.getScenarioType() != null) {
            prompt.setScenarioType(request.getScenarioType());
        }
        if (request.getJobRoleCode() != null || request.getJobRole() != null) {
            // 作用：
            // 修改 Prompt 时只要涉及岗位字段，就必须重新走岗位配置校验，
            // 避免 job_role_code 和 job_role_name 出现脱节。
            SysJobRole configuredJobRole = resolvePromptJobRole(request.getJobRoleCode(), request.getJobRole());
            prompt.setJobRoleCode(configuredJobRole.getRoleCode());
            prompt.setJobRole(configuredJobRole.getRoleName());
        }
        if (request.getDifficulty() != null) {
            prompt.setDifficulty(request.getDifficulty());
        }
        if (request.getPromptContent() != null) {
            prompt.setPromptContent(request.getPromptContent());
        }
        if (request.getActiveStatus() != null) {
            if (PromptConstants.ACTIVE ==(request.getActiveStatus())) {
                sysPromptService.deactivateOtherPrompts(
                        prompt.getScenarioType(),
                        prompt.getJobRoleCode(),
                        prompt.getDifficulty()
                );
            }
            prompt.setIsActive(request.getActiveStatus());
        }

        sysPromptService.updateById(prompt);
        log.info("Prompt updated, id: {}", request.getId());
        return Result.success("Prompt更新成功", null);
    }

    /**
     * 启用/禁用提示词模板
     *
     * @param id       提示词模板 ID
     * @param isActive 是否启用
     * @param authentication 认证对象
     * @return 空结果
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/prompts/{id}/active")
    public Result<Void> togglePromptActive(@PathVariable Long id,
                                             @RequestParam Integer isActive,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin toggle prompt active, id: {}, isActive: {}", id, isActive);
        if (isActive != 0 && isActive != 1) {
            throw new BusinessException("isActive 只能为 0 或 1");
        }

        SysPrompt prompt = sysPromptService.getById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt不存在");
        }

        if (PromptConstants.ACTIVE ==(isActive)) {
            sysPromptService.deactivateOtherPrompts(
                    prompt.getScenarioType(),
                    prompt.getJobRoleCode(),
                    prompt.getDifficulty()
            );
        }
        prompt.setIsActive(isActive);
        sysPromptService.updateById(prompt);

        log.info("Prompt active status updated, id: {}, isActive: {}", id, isActive);
        return Result.success("Prompt状态更新成功", null);
    }

    /**
     * 删除提示词模板（物理删除）
     *
     * 作用：
     * 管理员可以通过此接口物理删除 Prompt 模板，删除后数据无法恢复。
     * 注意：如果删除的 Prompt 正处于启用状态，需要谨慎操作。
     */
    @DeleteMapping("/prompts/{id}")
    public Result<Void> deletePrompt(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin delete prompt, id: {}", id);

        SysPrompt prompt = sysPromptService.getById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt不存在");
        }

        // 物理删除：直接移除
        sysPromptService.removeById(id);
        return Result.success("Prompt删除成功", null);
    }

    /**
     * 批量删除提示词模板（物理删除）
     *
     * 作用：
     * 管理员可以批量物理删除 Prompt 模板，删除后数据无法恢复。
     */
    @DeleteMapping("/prompts/batch")
    public Result<Void> deletePromptsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch delete prompts, ids: {}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的Prompt");
        }

        // 物理删除：批量移除
        sysPromptService.removeByIds(ids, false);
        log.info("Batch delete prompts completed, count: {}", ids.size());
        return Result.success("批量删除成功", null);
    }

/**
     * 批量启用/禁用提示词模板
     *
     * 作用：
     * 管理员可以批量启用或禁用提示词模板。
     * 启用时会自动禁用同岗位同难度的其他模板，保证最多只有一个启用。
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/prompts/batch/active")
    public Result<Void> togglePromptsBatchActive(@Valid @RequestBody BatchActiveRequest request,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin batch toggle prompts active, ids: {}, isActive: {}", request.getIds(), request.getIsActive());

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("请选择要操作的Prompt");
        }

        // 如果是启用操作，需要处理同岗位同难度的互斥逻辑
        if (request.getIsActive() != null && request.getIsActive() == 1) {
            // 先收集所有要启用的prompt信息，按jobRoleCode+difficulty分组
            for (Long id : request.getIds()) {
                SysPrompt prompt = sysPromptService.getById(id);
                if (prompt != null) {
                    // 先禁用同岗位同难度的其他prompts
                    sysPromptService.deactivateOtherPrompts(
                            prompt.getScenarioType(),
                            prompt.getJobRoleCode(),
                            prompt.getDifficulty()
                    );
                    // 然后启用当前prompt
                    prompt.setIsActive(1);
                    sysPromptService.updateById(prompt);
                }
            }
        } else {
            // 禁用操作直接设置
            for (Long id : request.getIds()) {
                SysPrompt prompt = sysPromptService.getById(id);
                if (prompt != null) {
                    prompt.setIsActive(request.getIsActive());
                    sysPromptService.updateById(prompt);
                }
            }
        }
        log.info("Batch toggle prompts active completed, count: {}", request.getIds().size());
        return Result.success("批量更新成功", null);
    }

    // ==================== 用户管理接口 ====================

    /**
     * 查询用户列表
     *
     * @param authentication 认证对象
     * @return 用户列表
     */
    @GetMapping("/users")
    public Result<List<UserListResponse>> getUserList(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin get user list");

        List<SysUser> users = sysUserService.list();
        List<UserListResponse> responses = users.stream()
                .map(this::buildUserListResponse)
                .collect(Collectors.toList());

        log.info("User list fetched, count: {}", responses.size());
        return Result.success(responses);
    }

    /**
     * 修改用户状态（封禁/解封）
     *
     * @param userId 用户ID
     * @param status 状态：1-正常，0-封禁
     * @param authentication 认证对象
     * @return 空结果
     */
    /**
     * 查询用户权益详情
     *
     * 作用：
     * 这个接口把用户身份字段和额度状态字段聚合在一起，
     * 方便管理端一次性查看当前会员状态、套餐和额度使用情况。
     */
    @GetMapping("/users/{userId}/rights")
    public Result<UserRightsResponse> getUserRights(@PathVariable Long userId,
                                                    Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin get user rights, userId: {}", userId);

        return Result.success(adminUserRightsService.getUserRights(userId));
    }

    /**
     * 修改用户权益
     *
     * 作用：
     * 管理员可以手工调整会员角色、套餐和到期时间。
     * 具体一致性规则交给服务层统一处理，并同步记录变更日志。
     */
    @PutMapping("/users/{userId}/rights")
    public Result<Void> updateUserRights(@PathVariable Long userId,
                                         @RequestBody UserRightsUpdateRequest request,
                                         Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin update user rights, operatorUserId: {}, userId: {}", adminUserId, userId);

        adminUserRightsService.updateUserRights(adminUserId, userId, request);
        return Result.success("用户权益更新成功", null);
    }

    @PutMapping("/users/{userId}/status")
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                          @RequestParam Integer status,
                                          Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin update user status, userId: {}, status: {}", userId, status);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(status);
        sysUserService.updateById(user);

        log.info("User status updated, userId: {}, status: {}", userId, status);
        return Result.success("用户状态更新成功", null);
    }

    /**
     * 批量启用/禁用用户
     *
     * 作用：
     * 管理员可以批量启用或禁用用户账号。
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/users/batch/status")
    @CacheEvict(value = {"auth:userInfo", "user:monthlyStats", "user:growthOverview"}, allEntries = true)
    public Result<Void> updateUsersBatchStatus(@Valid @RequestBody BatchActiveRequest request,
                                               Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin batch update users status, ids: {}, status: {}", request.getIds(), request.getIsActive());

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("请选择要操作的用户");
        }

        for (Long userId : request.getIds()) {
            SysUser user = sysUserService.getById(userId);
            if (user != null) {
                user.setStatus(request.getIsActive());
                sysUserService.updateById(user);
            }
        }
        log.info("Batch update users status completed, count: {}", request.getIds().size());
        return Result.success("批量更新成功", null);
    }

    // ==================== 额度管理接口 ====================

    /**
     * 查询用户额度
     *
     * @param userId 用户ID
     * @param authentication 认证对象
     * @return 用户额度信息
     */
    @GetMapping("/users/{userId}/quota")
    public Result<UserQuotaResponse> getUserQuota(@PathVariable Long userId,
                                                    Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin get user quota, userId: {}", userId);

        UserQuota quota = userQuotaService.getByUserId(userId);
        if (quota == null) {
            throw new BusinessException("用户额度记录不存在");
        }

        SysUser user = sysUserService.getById(userId);
        String username = user != null ? user.getUsername() : "";

        UserQuotaResponse response = UserQuotaResponse.builder()
                .id(quota.getId())
                .userId(quota.getUserId())
                .username(username)
                .totalInterviewUsed(quota.getTotalInterviewUsed())
                .totalResumeUsed(quota.getTotalResumeUsed())
                .dailyInterviewUsed(quota.getDailyInterviewUsed())
                .dailyResumeUsed(quota.getDailyResumeUsed())
                .interviewQuota(userQuotaService.getRemainingInterviewQuota(userId))
                .resumeQuota(userQuotaService.getRemainingResumeQuota(userId))
                .lastRefreshDate(quota.getLastRefreshDate())
                .createTime(quota.getCreateTime())
                .updateTime(quota.getUpdateTime())
                .build();

        log.info("User quota fetched, userId: {}", userId);
        return Result.success(response);
    }

    /**
     * 调整用户额度
     *
     * @param request 调整请求
     * @param authentication 认证对象
     * @return 空结果
     */
    @PutMapping("/users/quota")
    @CacheEvict(value = "auth:userInfo", key = "#request.userId")
    public Result<Void> updateUserQuota(@Valid @RequestBody UserQuotaUpdateRequest request,
                                         Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        checkAdminPermission(adminUserId);
        log.info("Admin update user quota, userId: {}", request.getUserId());

        UserQuota quota = userQuotaService.getByUserId(request.getUserId());
        if (quota == null) {
            throw new BusinessException("用户额度记录不存在");
        }

        if (request.getTotalInterviewUsed() != null) {
            quota.setTotalInterviewUsed(request.getTotalInterviewUsed());
        }
        if (request.getTotalResumeUsed() != null) {
            quota.setTotalResumeUsed(request.getTotalResumeUsed());
        }
        if (request.getDailyInterviewUsed() != null) {
            quota.setDailyInterviewUsed(request.getDailyInterviewUsed());
        }
        if (request.getDailyResumeUsed() != null) {
            quota.setDailyResumeUsed(request.getDailyResumeUsed());
        }
        if (request.getInterviewQuota() != null) {
            quota.setInterviewQuota(Math.max(0, request.getInterviewQuota()));
        }
        if (request.getResumeQuota() != null) {
            quota.setResumeQuota(Math.max(0, request.getResumeQuota()));
        }
        if (request.getLastRefreshDate() != null) {
            // 允许管理员修正日刷新边界，避免手工测试数据导致
            // 使用计数与 lastRefreshDate 出现漂移。
            quota.setLastRefreshDate(request.getLastRefreshDate());
        }

        userQuotaService.updateById(quota);
        log.info("User quota updated, userId: {}", request.getUserId());
        return Result.success("用户额度调整成功", null);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验管理员权限
     *
     * @param userId 用户ID
     */
    private void checkAdminPermission(Long userId) {
        SysUser user = sysUserService.getById(userId);
        Integer role = user != null ? user.getRole() : null;
        if (role == null || role != UserRoleConstants.ROLE_ADMIN) {
            log.warn("Non-admin user access denied, userId: {}, role: {}", userId, role);
            throw new BusinessException("无权限访问");
        }
        log.debug("Admin permission verified, userId: {}", userId);
    }

    /**
     * 构建提示词模板响应对象
     *
     * @param prompt 提示词模板实体
     * @return 提示词模板响应
     */
    /**
     * 构建 AI 引擎配置响应对象
     *
     * 作用：
     * 管理端列表直接展示后台配置，但 apiKey 属于敏感字段，必须在这里统一脱敏，
     * 避免原始密钥通过接口返回给前端。
     */
    private AiEngineConfigResponse buildAiEngineConfigResponse(SysAiEngineConfig config) {
        return AiEngineConfigResponse.builder()
                .id(config.getId())
                .engineCode(config.getEngineCode())
                .engineName(config.getEngineName())
                .providerType(config.getProviderType())
                .businessType(config.getBusinessType())
                .businessTypeDesc(getAiBusinessTypeDesc(config.getBusinessType()))
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(config.getApiKey()))
                .supportsMultimodal(config.getSupportsMultimodal())
                .thinkingMode(config.getThinkingMode())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeoutMs(config.getTimeoutMs())
                .isActive(config.getIsActive())
                .isActiveDesc(getActiveDesc(config.getIsActive()))
                .sort(config.getSort())
                .remark(config.getRemark())
                .createTime(config.getCreateTime())
                .updateTime(config.getUpdateTime())
                .build();
    }

    private PromptResponse buildPromptResponse(SysPrompt prompt) {
        return PromptResponse.builder()
                .id(prompt.getId())
                .scenarioType(prompt.getScenarioType())
                .scenarioTypeDesc(getScenarioTypeDesc(prompt.getScenarioType()))
                .jobRoleCode(prompt.getJobRoleCode())
                .jobRoleName(prompt.getJobRole())
                .jobRole(prompt.getJobRole())
                .difficulty(prompt.getDifficulty())
                .difficultyDesc(getDifficultyDesc(prompt.getDifficulty()))
                .promptContent(prompt.getPromptContent())
                .isActive(prompt.getIsActive())
                .isActiveDesc(getActiveDesc(prompt.getIsActive()))
                .createTime(prompt.getCreateTime())
                .updateTime(prompt.getUpdateTime())
                .build();
    }

    /**
     * 构建岗位配置响应
     */
    private JobRoleResponse buildJobRoleResponse(SysJobRole jobRole) {
        return JobRoleResponse.builder()
                .id(jobRole.getId())
                .roleCode(jobRole.getRoleCode())
                .roleName(jobRole.getRoleName())
                .interviewTag(jobRole.getInterviewTag())
                .tagType(jobRole.getTagType())
                .isActive(jobRole.getIsActive())
                .isActiveDesc(getActiveDesc(jobRole.getIsActive()))
                .sort(jobRole.getSort())
                .createTime(jobRole.getCreateTime())
                .updateTime(jobRole.getUpdateTime())
                .build();
    }

    /**
     * 构建用户列表响应对象
     *
     * @param user 用户实体
     * @return 用户列表响应
     */
private UserListResponse buildUserListResponse(SysUser user) {
        // 角色展示要按"当前是否仍是有效会员"判断，避免会员过期后仍显示为会员用户。
        boolean vipActive = isVipActive(user);
        return UserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .roleDesc(getRoleDesc(user.getRole(), vipActive))
                .status(user.getStatus())
                .statusDesc(getUserStatusDesc(user.getStatus()))
                .vipExpireTime(user.getVipExpireTime())
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * 获取场景类型描述
     *
     * @param scenarioType 场景类型
     * @return 描述
     */
    private String getScenarioTypeDesc(Integer scenarioType) {
        return switch (scenarioType) {
            case PromptConstants.SCENARIO_INTERVIEW -> "面试系统设定";
            case PromptConstants.SCENARIO_RESUME_DIAGNOSIS -> "简历诊断设定";
            default -> "未知";
        };
    }

    /**
     * 获取难度描述
     *
     * @param difficulty 难度级别
     * @return 描述
     */
    /**
     * 获取 AI 引擎业务类型描述
     */
    private String getAiBusinessTypeDesc(String businessType) {
        return switch (businessType) {
            case AiEngineConstants.BUSINESS_TYPE_INTERVIEW -> "模拟面试";
            case AiEngineConstants.BUSINESS_TYPE_RESUME -> "简历诊断";
            default -> "未知";
        };
    }

    private String getDifficultyDesc(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            default -> "未知";
        };
    }

    /**
     * 获取启用状态描述
     *
     * @param isActive 启用状态
     * @return 描述
     */
    private String getActiveDesc(Integer isActive) {
        return isActive != null && isActive == PromptConstants.ACTIVE ? "启用" : "禁用";
    }

    /**
     * 获取角色描述
     *
     * @param role 角色
     * @return 描述
     */
    private String getRoleDesc(Integer role, boolean vipActive) {
        return switch (role) {
            case UserRoleConstants.ROLE_NORMAL -> "普通用户";
            // VIP 角色但已过期时，管理端展示应回落为普通用户语义，避免误判用户权益状态。
            case UserRoleConstants.ROLE_VIP -> vipActive ? "会员用户" : "普通用户（会员已过期）";
            case UserRoleConstants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
    }

    /**
     * 计算用户当前是否处于“有效 VIP”状态。
     *
     * 作用：
     * 列表接口直接用实体字段判定，避免额外查询带来的 N+1 开销。
     */
    private boolean isVipActive(SysUser user) {
        if (user == null || user.getRole() == null || user.getRole() != UserRoleConstants.ROLE_VIP) {
            return false;
        }
        LocalDateTime vipExpireTime = user.getVipExpireTime();
        return vipExpireTime != null && vipExpireTime.isAfter(LocalDateTime.now());
    }

    /**
     * 获取用户状态描述
     *
     * @param status 状态
     * @return 描述
     */
    private String getUserStatusDesc(Integer status) {
        return status == 1 ? "正常" : "封禁";
    }

    /**
     * 解析提示词模板使用的岗位配置
     *
     * 作用：
     * 让提示词模板创建/编辑与 sys_job_role 真正联动。
     * 当前采用兼容升级方案：
     * 1. 优先使用 jobRoleCode
     * 2. 如果旧请求只传 jobRole 名称，则按名称回查岗位配置
     * 3. 两者都找不到时，拒绝保存
     *
     * 这样可以避免提示词模板岗位字段继续使用自由输入或游离字符串。
     */
    private SysJobRole resolvePromptJobRole(String jobRoleCode, String jobRoleName) {
        SysJobRole configuredJobRole = null;

        if (jobRoleCode != null && !jobRoleCode.trim().isEmpty()) {
            configuredJobRole = sysJobRoleService.getByRoleCode(jobRoleCode);
        } else if (jobRoleName != null && !jobRoleName.trim().isEmpty()) {
            configuredJobRole = sysJobRoleService.getByRoleName(jobRoleName);
        }

        if (configuredJobRole == null) {
            throw new BusinessException("Prompt 适用岗位不存在，请从岗位配置中选择合法岗位");
        }

        return configuredJobRole;
    }

    /**
     * 校验岗位编码和岗位名称唯一性
     *
     * 作用：
     * 岗位选项要作为全局唯一配置源，因此编码和名称都必须唯一，
     * 否则用户端下拉展示、面试创建校验和模板绑定都会产生歧义。
     */
    private void validateJobRoleUniqueness(String roleCode, String roleName, Long excludeId) {
        if (sysJobRoleService.existsByRoleCode(roleCode, excludeId)) {
            throw new BusinessException("岗位编码已存在");
        }
        if (sysJobRoleService.existsByRoleName(roleName, excludeId)) {
            throw new BusinessException("岗位名称已存在");
        }
    }

    /**
     * 校验 AI 引擎编码唯一性
     *
     * 作用：
     * engineCode 是后台配置的稳定标识，后续业务如果按编码引用配置，必须保证它全局唯一。
     */
    private void validateAiEngineCodeUniqueness(String engineCode, Long excludeId) {
        if (sysAiEngineConfigService.existsByEngineCode(engineCode, excludeId)) {
            throw new BusinessException("AI 引擎编码已存在");
        }
    }

    /**
     * 对必填字符串执行 trim 后校验
     */
    private String normalizeRequiredValue(String value, String errorMessage) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(errorMessage);
        }
        return normalized;
    }

    /**
     * 归一化 businessType
     *
     * 作用：
     * businessType 是“同业务只能启用一个配置”的分组键，必须统一成 trim + lowercase，
     * 避免因为大小写或前后空格不同而被后端误判成两个不同业务类型。
     */
    private String normalizeBusinessType(String businessType) {
        String normalized = trimToNull(businessType);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

/**
     * 验证是否为脱敏格式的 API Key。
     *
     * 作用：
     * 防止前端把列表返回的脱敏值（如 "sk-****abcd"）提交回后端，
     * 导致真实 API Key 被覆盖为脱敏值。
     *
     * 判断逻辑：
     * - 如果包含 "****" 且长度明显短于真实 key（真实 key 通常 > 20 字符），则判定为脱敏值
     *
     * @param apiKey 待验证的 API Key
     * @return true 表示是脱敏格式，false 表示可能是真实值
     */
    private boolean isMaskedApiKey(String apiKey) {
        if (apiKey == null) {
            return false;
        }
        String trimmed = apiKey.trim();
        // 不包含脱敏标记，不是脱敏格式
        if (!trimmed.contains("****")) {
            return false;
        }
        // 【关键修复】真实 API Key 通常 > 20 字符，如果 <= 20 字符且包含 "****"，很可能是脱敏值
        if (trimmed.length() <= 20) {
            return true;
        }
        return false;
    }

    /**
     * 将 API Key 脱敏返回。
     *
     * 作用：
     * 原始密钥只应该保存在数据库中，管理端列表返回时必须遮罩，
     * 防止前端日志、缓存或截图直接泄露密钥。
     */
    private String maskApiKey(String apiKey) {
        String normalized = trimToNull(apiKey);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        if (normalized.length() <= 8) {
            return normalized.substring(0, 2) + "****";
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
