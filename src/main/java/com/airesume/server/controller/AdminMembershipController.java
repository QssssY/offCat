package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.BatchValidator;
import com.airesume.server.dto.admin.BatchActiveRequest;
import com.airesume.server.entity.MembershipOrder;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.MembershipOrderService;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/membership")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMembershipController {

    private final MembershipPlanService membershipPlanService;
    private final MembershipOrderService membershipOrderService;
    private final SysUserService sysUserService;

    @GetMapping("/plans")
    public Result<Map<String, Object>> getPlanList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get membership plan list, page: {}, size: {}", page, size);
        Page<MembershipPlan> pageParam = new Page<>(page, size);
        Page<MembershipPlan> result = membershipPlanService.lambdaQuery()
                .orderByAsc(MembershipPlan::getSort)
                .page(pageParam);
        List<MembershipPlanResponse> records = result.getRecords().stream()
                .map(this::buildPlanResponse)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @PostMapping("/plans")
    public Result<Long> createPlan(@Valid @RequestBody MembershipPlanCreateRequest request,
                                    Authentication authentication) {
        log.info("Admin create membership plan, code: {}, name: {}", request.getPlanCode(), request.getPlanName());

        boolean exists = membershipPlanService.lambdaQuery()
                .eq(MembershipPlan::getPlanCode, request.getPlanCode().trim())
                .count() > 0;
        if (exists) {
            throw new BusinessException("套餐编码已存在");
        }

        MembershipPlan plan = new MembershipPlan();
        plan.setPlanCode(request.getPlanCode().trim());
        plan.setPlanName(request.getPlanName().trim());
        plan.setDescription(request.getDescription());
        plan.setPriceAmount(request.getPriceAmount());
        plan.setDurationDays(request.getDurationDays());
        plan.setResumeQuota(request.getResumeQuota());
        plan.setInterviewQuota(request.getInterviewQuota());
        plan.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        plan.setSort(request.getSort() != null ? request.getSort() : 0);
        membershipPlanService.save(plan);

        log.info("Membership plan created, id: {}", plan.getId());
        return Result.success("套餐创建成功", plan.getId());
    }

    @PutMapping("/plans")
    public Result<Void> updatePlan(@Valid @RequestBody MembershipPlanUpdateRequest request,
                                    Authentication authentication) {
        log.info("Admin update membership plan, id: {}", request.getId());
        MembershipPlan plan = membershipPlanService.getById(request.getId());
        if (plan == null) {
            throw new BusinessException("套餐不存在");
        }

        if (request.getPlanCode() != null) {
            boolean exists = membershipPlanService.lambdaQuery()
                    .eq(MembershipPlan::getPlanCode, request.getPlanCode().trim())
                    .ne(MembershipPlan::getId, request.getId())
                    .count() > 0;
            if (exists) throw new BusinessException("套餐编码已存在");
            plan.setPlanCode(request.getPlanCode().trim());
        }
        if (request.getPlanName() != null) plan.setPlanName(request.getPlanName().trim());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getPriceAmount() != null) plan.setPriceAmount(request.getPriceAmount());
        if (request.getDurationDays() != null) plan.setDurationDays(request.getDurationDays());
        if (request.getResumeQuota() != null) plan.setResumeQuota(request.getResumeQuota());
        if (request.getInterviewQuota() != null) plan.setInterviewQuota(request.getInterviewQuota());
        if (request.getStatus() != null) plan.setStatus(request.getStatus());
        if (request.getSort() != null) plan.setSort(request.getSort());

        membershipPlanService.updateById(plan);
        return Result.success("套餐更新成功", null);
    }

    @PutMapping("/plans/{id}/active")
    public Result<Void> togglePlanActive(@PathVariable Long id, @RequestParam Integer status,
                                          Authentication authentication) {
        MembershipPlan plan = membershipPlanService.getById(id);
        if (plan == null) throw new BusinessException("套餐不存在");
        plan.setStatus(status);
        membershipPlanService.updateById(plan);
        return Result.success("套餐状态更新成功", null);
    }

    @PutMapping("/plans/batch/active")
    public Result<Void> togglePlansBatchActive(@Valid @RequestBody BatchActiveRequest request,
                                               Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(request.getIds());
        log.info("Admin batch toggle membership plans active, ids: {}, status: {}", safeIds, request.getIsActive());
        List<MembershipPlan> plans = membershipPlanService.listByIds(safeIds);
        for (MembershipPlan plan : plans) {
            // 批量启停只改状态字段，保留套餐价格、额度、排序等业务配置。
            plan.setStatus(request.getIsActive());
            membershipPlanService.updateById(plan);
        }
        return Result.success("套餐状态批量更新成功", null);
    }

    @DeleteMapping("/plans/{id}")
    public Result<Void> deletePlan(@PathVariable Long id, Authentication authentication) {
        log.info("Admin delete membership plan, id: {}", id);
        membershipPlanService.removeById(id);
        return Result.success("套餐删除成功", null);
    }

    @PostMapping("/plans/batch-delete")
    public Result<Void> deletePlansBatch(@RequestBody List<Long> ids, Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch delete membership plans, ids: {}", safeIds);
        membershipPlanService.removeByIds(safeIds);
        return Result.success("套餐批量删除成功", null);
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> getOrderList(
            @RequestParam(required = false) String orderStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get membership order list, status: {}, page: {}, size: {}", orderStatus, page, size);
        Page<MembershipOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MembershipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(MembershipOrder::getCreateTime);
        if (orderStatus != null && !orderStatus.isBlank()) {
            wrapper.eq(MembershipOrder::getOrderStatus, orderStatus);
        }
        Page<MembershipOrder> result = membershipOrderService.page(pageParam, wrapper);
        List<MembershipOrderResponse> records = result.getRecords().stream()
                .map(this::buildOrderResponse).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @Data
    public static class MembershipPlanCreateRequest {
        @NotBlank(message = "套餐编码不能为空")
        private String planCode;
        @NotBlank(message = "套餐名称不能为空")
        private String planName;
        private String description;
        @NotNull(message = "价格不能为空")
        private BigDecimal priceAmount;
        @NotNull(message = "有效天数不能为空")
        private Integer durationDays;
        @NotNull(message = "简历额度不能为空")
        private Integer resumeQuota;
        @NotNull(message = "面试额度不能为空")
        private Integer interviewQuota;
        private Integer status;
        private Integer sort;
    }

    @Data
    public static class MembershipPlanUpdateRequest {
        @NotNull(message = "ID不能为空")
        private Long id;
        private String planCode;
        private String planName;
        private String description;
        private BigDecimal priceAmount;
        private Integer durationDays;
        private Integer resumeQuota;
        private Integer interviewQuota;
        private Integer status;
        private Integer sort;
    }

    @Data
    @AllArgsConstructor
    public static class MembershipPlanResponse {
        private Long id;
        private String planCode;
        private String planName;
        private String description;
        private BigDecimal priceAmount;
        private Integer durationDays;
        private Integer resumeQuota;
        private Integer interviewQuota;
        private Integer status;
        private String statusDesc;
        private Integer sort;
        private LocalDateTime createTime;
    }

    @Data
    @AllArgsConstructor
    public static class MembershipOrderResponse {
        private Long id;
        private String orderNo;
        private Long userId;
        private String username;
        private String planCode;
        private String planName;
        private String orderStatus;
        private BigDecimal orderAmount;
        private Integer durationDays;
        private Integer grantedResumeQuota;
        private Integer grantedInterviewQuota;
        private LocalDateTime paidAt;
        private LocalDateTime createTime;
    }

    private MembershipPlanResponse buildPlanResponse(MembershipPlan plan) {
        return new MembershipPlanResponse(
                plan.getId(), plan.getPlanCode(), plan.getPlanName(),
                plan.getDescription(), plan.getPriceAmount(), plan.getDurationDays(),
                plan.getResumeQuota(), plan.getInterviewQuota(), plan.getStatus(),
                plan.getStatus() == 1 ? "启用" : "禁用", plan.getSort(), plan.getCreateTime());
    }

    private MembershipOrderResponse buildOrderResponse(MembershipOrder order) {
        SysUser user = sysUserService.getById(order.getUserId());
        return new MembershipOrderResponse(
                order.getId(), order.getOrderNo(), order.getUserId(),
                user != null ? user.getUsername() : "未知",
                order.getPlanCode(), order.getPlanName(), order.getOrderStatus(),
                order.getOrderAmount(), order.getDurationDays(),
                order.getGrantedResumeQuota(), order.getGrantedInterviewQuota(),
                order.getPaidAt(), order.getCreateTime());
    }

}
