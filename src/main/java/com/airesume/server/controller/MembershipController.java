package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.membership.MembershipUpgradeRequest;
import com.airesume.server.service.MembershipService;
import com.airesume.server.vo.membership.MembershipPlanVO;
import com.airesume.server.vo.membership.MembershipUpgradeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/plans")
    public Result<List<MembershipPlanVO>> listPlans() {
        return Result.success(membershipService.listPlans());
    }

    @PostMapping("/upgrade/mock")
    public Result<MembershipUpgradeVO> mockUpgrade(@Valid @RequestBody MembershipUpgradeRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Mock membership upgrade request, userId: {}, planCode: {}", userId, request.getPlanCode());
        MembershipUpgradeVO response = membershipService.mockUpgrade(userId, request);
        return Result.success("Membership upgraded successfully", response);
    }
}
