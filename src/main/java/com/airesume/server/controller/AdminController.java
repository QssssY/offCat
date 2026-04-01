package com.airesume.server.controller;

import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.*;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.infrastructure.security.JwtUtil;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端控制器
 * 提供Prompt管理、用户管理、额度管理等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysPromptService sysPromptService;
    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
    private final JwtUtil jwtUtil;

    // ==================== Prompt管理接口 ====================

    /**
     * 查询Prompt模板列表
     *
     * @param token JWT Token
     * @return Prompt列表
     */
    @GetMapping("/prompts")
    public Result<List<PromptResponse>> getPromptList(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        log.info("Admin get prompt list");

        List<SysPrompt> prompts = sysPromptService.list();
        List<PromptResponse> responses = prompts.stream()
                .map(this::buildPromptResponse)
                .collect(Collectors.toList());

        log.info("Prompt list fetched, count: {}", responses.size());
        return Result.success(responses);
    }

    /**
     * 新增Prompt模板
     *
     * @param request 创建请求
     * @param token   JWT Token
     * @return 新增的Prompt ID
     */
    @PostMapping("/prompts")
    public Result<Long> createPrompt(@Valid @RequestBody PromptCreateRequest request,
                                       @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        log.info("Admin create prompt, scenarioType: {}, jobRole: {}, difficulty: {}",
                request.getScenarioType(), request.getJobRole(), request.getDifficulty());

        SysPrompt prompt = new SysPrompt();
        prompt.setScenarioType(request.getScenarioType());
        prompt.setJobRole(request.getJobRole());
        prompt.setDifficulty(request.getDifficulty());
        prompt.setPromptContent(request.getPromptContent());
        prompt.setIsActive(PromptConstants.ACTIVE);
        sysPromptService.save(prompt);

        log.info("Prompt created, id: {}", prompt.getId());
        return Result.success("Prompt创建成功", prompt.getId());
    }

    /**
     * 修改Prompt模板
     *
     * @param request 更新请求
     * @param token   JWT Token
     * @return 空结果
     */
    @PutMapping("/prompts")
    public Result<Void> updatePrompt(@Valid @RequestBody PromptUpdateRequest request,
                                      @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        log.info("Admin update prompt, id: {}", request.getId());

        SysPrompt prompt = sysPromptService.getById(request.getId());
        if (prompt == null) {
            throw new BusinessException("Prompt不存在");
        }

        if (request.getScenarioType() != null) {
            prompt.setScenarioType(request.getScenarioType());
        }
        if (request.getJobRole() != null) {
            prompt.setJobRole(request.getJobRole());
        }
        if (request.getDifficulty() != null) {
            prompt.setDifficulty(request.getDifficulty());
        }
        if (request.getPromptContent() != null) {
            prompt.setPromptContent(request.getPromptContent());
        }
        if (request.getIsActive() != null) {
            prompt.setIsActive(request.getIsActive());
        }

        sysPromptService.updateById(prompt);
        log.info("Prompt updated, id: {}", request.getId());
        return Result.success("Prompt更新成功", null);
    }

    /**
     * 启用/禁用Prompt模板
     *
     * @param id       Prompt ID
     * @param isActive 是否启用
     * @param token    JWT Token
     * @return 空结果
     */
    @PutMapping("/prompts/{id}/active")
    public Result<Void> togglePromptActive(@PathVariable Long id,
                                             @RequestParam Integer isActive,
                                             @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        log.info("Admin toggle prompt active, id: {}, isActive: {}", id, isActive);

        SysPrompt prompt = sysPromptService.getById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt不存在");
        }

        prompt.setIsActive(isActive);
        sysPromptService.updateById(prompt);

        log.info("Prompt active status updated, id: {}, isActive: {}", id, isActive);
        return Result.success("Prompt状态更新成功", null);
    }

    // ==================== 用户管理接口 ====================

    /**
     * 查询用户列表
     *
     * @param token JWT Token
     * @return 用户列表
     */
    @GetMapping("/users")
    public Result<List<UserListResponse>> getUserList(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
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
     * @param token  JWT Token
     * @return 空结果
     */
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                          @RequestParam Integer status,
                                          @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
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

    // ==================== 额度管理接口 ====================

    /**
     * 查询用户额度
     *
     * @param userId 用户ID
     * @param token  JWT Token
     * @return 用户额度信息
     */
    @GetMapping("/users/{userId}/quota")
    public Result<UserQuotaResponse> getUserQuota(@PathVariable Long userId,
                                                    @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
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
     * @param token   JWT Token
     * @return 空结果
     */
    @PutMapping("/users/quota")
    public Result<Void> updateUserQuota(@Valid @RequestBody UserQuotaUpdateRequest request,
                                         @RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
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

        userQuotaService.updateById(quota);
        log.info("User quota updated, userId: {}", request.getUserId());
        return Result.success("用户额度调整成功", null);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验管理员权限
     *
     * @param token JWT Token
     */
    private void checkAdminPermission(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);
        if (role == null || role != UserRoleConstants.ROLE_ADMIN) {
            log.warn("Non-admin user access denied, userId: {}, role: {}", userId, role);
            throw new BusinessException("无权限访问");
        }
        log.debug("Admin permission verified, userId: {}", userId);
    }

    /**
     * 构建Prompt响应对象
     *
     * @param prompt Prompt实体
     * @return Prompt响应
     */
    private PromptResponse buildPromptResponse(SysPrompt prompt) {
        return PromptResponse.builder()
                .id(prompt.getId())
                .scenarioType(prompt.getScenarioType())
                .scenarioTypeDesc(getScenarioTypeDesc(prompt.getScenarioType()))
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
     * 构建用户列表响应对象
     *
     * @param user 用户实体
     * @return 用户列表响应
     */
    private UserListResponse buildUserListResponse(SysUser user) {
        return UserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .roleDesc(getRoleDesc(user.getRole()))
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
    private String getRoleDesc(Integer role) {
        return switch (role) {
            case UserRoleConstants.ROLE_NORMAL -> "普通用户";
            case UserRoleConstants.ROLE_VIP -> "会员用户";
            case UserRoleConstants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
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
}
