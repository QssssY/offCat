package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.BatchActiveRequest;
import com.airesume.server.entity.MembershipOrder;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.service.MembershipOrderService;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMembershipControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private MembershipPlanService membershipPlanService;
    @Mock private MembershipOrderService membershipOrderService;
    @Mock private SysUserService sysUserService;
    @Mock private Authentication authentication;

    private AdminMembershipController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminMembershipController(membershipPlanService, membershipOrderService, sysUserService);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getPlanListShouldReturnPagedPlans() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(100L);
        plan.setPlanCode("vip_month");
        plan.setPlanName("Monthly VIP");
        plan.setPriceAmount(BigDecimal.valueOf(29.90));
        plan.setDurationDays(30);
        plan.setResumeQuota(10);
        plan.setInterviewQuota(10);
        plan.setStatus(1);
        plan.setSort(1);
        plan.setCreateTime(LocalDateTime.now());

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        Page<MembershipPlan> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(plan));
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).orderByAsc(any(SFunction.class));
        doReturn(pageResult).when(wrapper).page(any(Page.class));

        Result<Map<String, Object>> result = controller.getPlanList(1, 20, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<AdminMembershipController.MembershipPlanResponse> records =
                (List<AdminMembershipController.MembershipPlanResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("vip_month", records.get(0).getPlanCode());
        assertEquals(1, result.getData().get("total"));
        assertEquals(1, result.getData().get("page"));
        assertEquals(20, result.getData().get("size"));
    }

    @Test
    void createPlanShouldReturnNewId() {
        AdminMembershipController.MembershipPlanCreateRequest request = new AdminMembershipController.MembershipPlanCreateRequest();
        request.setPlanCode("vip_year");
        request.setPlanName("Yearly VIP");
        request.setPriceAmount(BigDecimal.valueOf(299));
        request.setDurationDays(365);
        request.setResumeQuota(120);
        request.setInterviewQuota(120);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(0L);
        when(membershipPlanService.save(any(MembershipPlan.class))).thenAnswer(invocation -> {
            MembershipPlan saved = invocation.getArgument(0);
            saved.setId(200L);
            return true;
        });

        Result<Long> result = controller.createPlan(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(200L, result.getData());
        verify(membershipPlanService).save(any(MembershipPlan.class));
    }

    @Test
    void createPlanShouldRejectWhenEnabledPlanLimitReached() {
        AdminMembershipController.MembershipPlanCreateRequest request = new AdminMembershipController.MembershipPlanCreateRequest();
        request.setPlanCode("vip_extra");
        request.setPlanName("Extra VIP");
        request.setPriceAmount(BigDecimal.valueOf(399));
        request.setDurationDays(365);
        request.setResumeQuota(30);
        request.setInterviewQuota(40);
        request.setStatus(1);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> codeWrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> enabledWrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(codeWrapper, enabledWrapper);
        doReturn(codeWrapper).when(codeWrapper).eq(any(SFunction.class), any());
        when(codeWrapper.count()).thenReturn(0L);
        doReturn(enabledWrapper).when(enabledWrapper).eq(any(SFunction.class), any());
        when(enabledWrapper.count()).thenReturn(6L);

        assertThrows(BusinessException.class, () -> controller.createPlan(request, authentication));
    }

    @Test
    void createPlanShouldThrowWhenCodeExists() {
        AdminMembershipController.MembershipPlanCreateRequest request = new AdminMembershipController.MembershipPlanCreateRequest();
        request.setPlanCode("vip_month");
        request.setPlanName("Monthly VIP");
        request.setPriceAmount(BigDecimal.valueOf(29.90));
        request.setDurationDays(30);
        request.setResumeQuota(10);
        request.setInterviewQuota(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.createPlan(request, authentication));
    }

    @Test
    void updatePlanShouldSucceed() {
        AdminMembershipController.MembershipPlanUpdateRequest request = new AdminMembershipController.MembershipPlanUpdateRequest();
        request.setId(100L);
        request.setPlanName("Yearly VIP");

        MembershipPlan existingPlan = new MembershipPlan();
        existingPlan.setId(100L);
        existingPlan.setPlanCode("vip_month");
        when(membershipPlanService.getById(100L)).thenReturn(existingPlan);

        Result<Void> result = controller.updatePlan(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(membershipPlanService).updateById(existingPlan);
    }

    @Test
    void updatePlanShouldThrowWhenNotFound() {
        AdminMembershipController.MembershipPlanUpdateRequest request = new AdminMembershipController.MembershipPlanUpdateRequest();
        request.setId(999L);
        when(membershipPlanService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.updatePlan(request, authentication));
    }

    @Test
    void togglePlanActiveShouldUpdateStatus() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(100L);
        plan.setStatus(0);
        when(membershipPlanService.getById(100L)).thenReturn(plan);
        mockEnabledPlanCount(5L);

        Result<Void> result = controller.togglePlanActive(100L, 1, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(1, plan.getStatus());
        verify(membershipPlanService).updateById(plan);
    }

    @Test
    void togglePlanActiveShouldRejectWhenEnabledPlanLimitReached() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(100L);
        plan.setStatus(0);
        when(membershipPlanService.getById(100L)).thenReturn(plan);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(6L);

        assertThrows(BusinessException.class, () -> controller.togglePlanActive(100L, 1, authentication));
    }

    @Test
    void deletePlanShouldRemove() {
        Result<Void> result = controller.deletePlan(100L, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(membershipPlanService).removeById(100L);
    }

    @Test
    void togglePlansBatchActiveShouldUpdateStatuses() {
        MembershipPlan plan1 = new MembershipPlan();
        plan1.setId(100L);
        plan1.setStatus(0);
        MembershipPlan plan2 = new MembershipPlan();
        plan2.setId(200L);
        plan2.setStatus(0);
        BatchActiveRequest request = new BatchActiveRequest();
        request.setIds(List.of(100L, 200L));
        request.setIsActive(1);
        when(membershipPlanService.listByIds(List.of(100L, 200L))).thenReturn(List.of(plan1, plan2));
        mockEnabledPlanCount(4L);

        Result<Void> result = controller.togglePlansBatchActive(request, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(1, plan1.getStatus());
        assertEquals(1, plan2.getStatus());
        verify(membershipPlanService).updateById(plan1);
        verify(membershipPlanService).updateById(plan2);
    }

    @Test
    void togglePlansBatchActiveShouldRejectWhenEnabledPlanLimitWouldBeExceeded() {
        MembershipPlan plan1 = new MembershipPlan();
        plan1.setId(100L);
        plan1.setStatus(0);
        MembershipPlan plan2 = new MembershipPlan();
        plan2.setId(200L);
        plan2.setStatus(0);
        BatchActiveRequest request = new BatchActiveRequest();
        request.setIds(List.of(100L, 200L));
        request.setIsActive(1);
        when(membershipPlanService.listByIds(List.of(100L, 200L))).thenReturn(List.of(plan1, plan2));

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(5L);

        assertThrows(BusinessException.class, () -> controller.togglePlansBatchActive(request, authentication));
    }

    @Test
    void togglePlansBatchActiveShouldRejectEmptyIds() {
        BatchActiveRequest request = new BatchActiveRequest();
        request.setIds(List.of());
        request.setIsActive(1);

        assertThrows(BusinessException.class, () -> controller.togglePlansBatchActive(request, authentication));
    }

    @Test
    void deletePlansBatchShouldRemoveIds() {
        Result<Void> result = controller.deletePlansBatch(List.of(100L, 200L), authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        verify(membershipPlanService).removeByIds(List.of(100L, 200L));
    }

    private void mockEnabledPlanCount(long enabledCount) {
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<MembershipPlan> wrapper = mock(
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(membershipPlanService.lambdaQuery()).thenReturn(wrapper);
        doReturn(wrapper).when(wrapper).eq(any(SFunction.class), any());
        when(wrapper.count()).thenReturn(enabledCount);
    }

    @Test
    void getOrderListShouldReturnPagedOrders() {
        MembershipOrder order = new MembershipOrder();
        order.setId(1L);
        order.setOrderNo("ORD20250101001");
        order.setUserId(10L);
        order.setPlanCode("vip_month");
        order.setPlanName("Monthly VIP");
        order.setOrderStatus("PAID");
        order.setOrderAmount(BigDecimal.valueOf(29.90));

        Page<MembershipOrder> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(order));
        when(membershipOrderService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
        when(sysUserService.getById(10L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.getOrderList(null, 1, 20, authentication);

        assertEquals(CODE_SUCCESS, result.getCode());
        @SuppressWarnings("unchecked")
        List<AdminMembershipController.MembershipOrderResponse> records =
                (List<AdminMembershipController.MembershipOrderResponse>) result.getData().get("records");
        assertEquals(1, records.size());
        assertEquals("ORD20250101001", records.get(0).getOrderNo());
        assertEquals(1, result.getData().get("total"));
        assertEquals(1, result.getData().get("page"));
        assertEquals(20, result.getData().get("size"));
    }
}
