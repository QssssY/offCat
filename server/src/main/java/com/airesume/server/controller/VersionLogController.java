package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.VersionLogResponse;
import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/version-logs")
@RequiredArgsConstructor
public class VersionLogController {

    private final SysVersionLogService sysVersionLogService;

    @GetMapping
    public Result<Map<String, Object>> getVersionLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 50));
        log.info("Get public version logs, page: {}, size: {}, safeSize: {}", page, size, safeSize);

        LambdaQueryWrapper<SysVersionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysVersionLog::getStatus, 1)
                .orderByDesc(SysVersionLog::getPublishedAt)
                .orderByDesc(SysVersionLog::getCreateTime);
        Page<SysVersionLog> result = sysVersionLogService.page(new Page<>(safePage, safeSize), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords().stream().map(this::buildResponse).collect(Collectors.toList()));
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    @GetMapping("/latest")
    public Result<List<VersionLogResponse>> getLatestVersionLogs(
            @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        log.info("Get latest version logs, limit: {}, safeLimit: {}", limit, safeLimit);
        List<SysVersionLog> logs = sysVersionLogService.getLatestPublished(safeLimit);
        return Result.success(logs.stream().map(this::buildResponse).collect(Collectors.toList()));
    }

    private VersionLogResponse buildResponse(SysVersionLog entity) {
        return VersionLogResponse.builder()
                .id(entity.getId())
                .version(entity.getVersion())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .typeDesc(getTypeDesc(entity.getType()))
                .status(entity.getStatus())
                .statusDesc("已发布")
                .publishedAt(entity.getPublishedAt())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private String getTypeDesc(String type) {
        return switch (type) {
            case "major" -> "大版本";
            case "minor" -> "小版本";
            case "patch" -> "修补";
            default -> "未知";
        };
    }
}
