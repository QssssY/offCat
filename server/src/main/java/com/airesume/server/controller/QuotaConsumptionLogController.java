package com.airesume.server.controller;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.quota.ConsumptionLogResponse;
import com.airesume.server.service.QuotaConsumptionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户额度消费记录控制器
 * 提供用户查询自己的额度消费明细接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user/quota")
@RequiredArgsConstructor
public class QuotaConsumptionLogController {

    private final QuotaConsumptionLogService consumptionLogService;

    /**
     * 查询当前用户的额度消费记录（分页 + 类型筛选）
     *
     * @param authentication 当前登录用户身份
     * @param quotaType      额度类型筛选（不传=全部）
     * @param pageNum        页码（默认1）
     * @param pageSize       每页条数（默认20）
     * @return 分页消费记录
     */
    @GetMapping("/consumption-log")
    public Result<PageResult<ConsumptionLogResponse>> getConsumptionLog(
            Authentication authentication,
            @RequestParam(required = false) String quotaType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        // 从认证对象获取userId，不接受客户端传入
        Long userId = (Long) authentication.getPrincipal();
        log.info("查询消费记录, userId: {}, quotaType: {}, page: {}, size: {}", userId, quotaType, pageNum, pageSize);

        int safePage = Math.max(1, pageNum);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        PageResult<ConsumptionLogResponse> result = consumptionLogService.getUserConsumptionLog(
                userId, quotaType, safePage, safeSize);
        return Result.success(result);
    }
}
