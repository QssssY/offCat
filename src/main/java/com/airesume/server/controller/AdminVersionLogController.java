package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.BatchValidator;
import com.airesume.server.dto.admin.VersionLogCreateRequest;
import com.airesume.server.dto.admin.VersionLogResponse;
import com.airesume.server.dto.admin.VersionLogUpdateRequest;
import com.airesume.server.entity.SysVersionLog;
import com.airesume.server.service.SysVersionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/version-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVersionLogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SysVersionLogService sysVersionLogService;

    @GetMapping
    public Result<Map<String, Object>> getVersionLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        String safeType = normalizeOptionalText(type);
        String safeKeyword = normalizeOptionalText(keyword);
        log.info("Admin get version log list, page: {}, size: {}, type: {}, status: {}, keyword: {}",
                page, size, safeType, status, safeKeyword);

        if (safeType != null && !isSupportedVersionType(safeType)) {
            return Result.error("版本类型仅支持 major/minor/patch");
        }
        if (status != null && status != 0 && status != 1) {
            return Result.error("版本状态仅支持 0/1");
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Page<SysVersionLog> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<SysVersionLog> wrapper = new LambdaQueryWrapper<>();
        // 管理端版本日志筛选统一走后端参数化查询，避免前端本地筛选导致分页总数失真。
        if (safeType != null) {
            wrapper.eq(SysVersionLog::getType, safeType);
        }
        if (status != null) {
            wrapper.eq(SysVersionLog::getStatus, status);
        }
        if (safeKeyword != null) {
            wrapper.and(query -> query.like(SysVersionLog::getTitle, safeKeyword)
                    .or()
                    .like(SysVersionLog::getContent, safeKeyword)
                    .or()
                    .like(SysVersionLog::getVersion, safeKeyword));
        }
        wrapper.orderByDesc(SysVersionLog::getCreateTime);
        Page<SysVersionLog> result = sysVersionLogService.page(pageParam, wrapper);
        List<VersionLogResponse> records = result.getRecords().stream()
                .map(this::buildResponse).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    @PostMapping
    public Result<Long> createVersionLog(@Valid @RequestBody VersionLogCreateRequest request,
                                          Authentication authentication) {
        log.info("Admin create version log, version: {}, title: {}", request.getVersion(), request.getTitle());

        boolean exists = sysVersionLogService.lambdaQuery()
                .eq(SysVersionLog::getVersion, request.getVersion().trim())
                .count() > 0;
        if (exists) {
            throw new BusinessException("版本号已存在");
        }

        SysVersionLog versionLog = new SysVersionLog();
        versionLog.setVersion(request.getVersion().trim());
        versionLog.setTitle(request.getTitle().trim());
        versionLog.setContent(request.getContent().trim());
        versionLog.setType(request.getType());
        int status = request.getStatus() != null ? request.getStatus() : 0;
        versionLog.setStatus(status);
        if (status == 1) {
            versionLog.setPublishedAt(LocalDateTime.now());
        }
        sysVersionLogService.save(versionLog);

        log.info("Version log created, id: {}", versionLog.getId());
        return Result.success("版本日志创建成功", versionLog.getId());
    }

    @PutMapping
    public Result<Void> updateVersionLog(@Valid @RequestBody VersionLogUpdateRequest request,
                                          Authentication authentication) {
        log.info("Admin update version log, id: {}", request.getId());

        SysVersionLog versionLog = sysVersionLogService.getById(request.getId());
        if (versionLog == null) {
            throw new BusinessException("版本日志不存在");
        }

        if (request.getVersion() != null) {
            boolean exists = sysVersionLogService.lambdaQuery()
                    .eq(SysVersionLog::getVersion, request.getVersion().trim())
                    .ne(SysVersionLog::getId, request.getId())
                    .count() > 0;
            if (exists) {
                throw new BusinessException("版本号已存在");
            }
            versionLog.setVersion(request.getVersion().trim());
        }
        if (request.getTitle() != null) versionLog.setTitle(request.getTitle().trim());
        if (request.getContent() != null) versionLog.setContent(request.getContent().trim());
        if (request.getType() != null) versionLog.setType(request.getType());
        if (request.getStatus() != null) {
            versionLog.setStatus(request.getStatus());
            if (request.getStatus() == 1 && versionLog.getPublishedAt() == null) {
                versionLog.setPublishedAt(LocalDateTime.now());
            }
        }

        sysVersionLogService.updateById(versionLog);
        log.info("Version log updated, id: {}", request.getId());
        return Result.success("版本日志更新成功", null);
    }

    @PutMapping("/{id}/publish")
    public Result<Void> publishVersionLog(@PathVariable Long id, Authentication authentication) {
        SysVersionLog versionLog = sysVersionLogService.getById(id);
        if (versionLog == null) {
            throw new BusinessException("版本日志不存在");
        }
        if (versionLog.getStatus() == 1) {
            return Result.error("版本日志已发布");
        }
        versionLog.setStatus(1);
        versionLog.setPublishedAt(LocalDateTime.now());
        sysVersionLogService.updateById(versionLog);
        return Result.success("版本日志发布成功", null);
    }

    @PutMapping("/batch/publish")
    public Result<Void> publishVersionLogsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch publish version logs, ids: {}", safeIds);
        List<SysVersionLog> versionLogs = sysVersionLogService.listByIds(safeIds);
        for (SysVersionLog versionLog : versionLogs) {
            // 批量发布只推进草稿记录，已发布记录保持原状态，避免重复操作报错。
            if (versionLog.getStatus() == 1) {
                continue;
            }
            versionLog.setStatus(1);
            versionLog.setPublishedAt(LocalDateTime.now());
            sysVersionLogService.updateById(versionLog);
        }
        return Result.success("版本日志批量发布成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteVersionLog(@PathVariable Long id, Authentication authentication) {
        log.info("Admin delete version log, id: {}", id);
        sysVersionLogService.removeById(id);
        return Result.success("版本日志删除成功", null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> deleteVersionLogsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch delete version logs, ids: {}", safeIds);
        sysVersionLogService.removeByIds(safeIds);
        return Result.success("版本日志批量删除成功", null);
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
                .statusDesc(entity.getStatus() == 1 ? "已发布" : "草稿")
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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isSupportedVersionType(String type) {
        return switch (type) {
            case "major", "minor", "patch" -> true;
            default -> false;
        };
    }
}
