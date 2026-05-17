package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.BatchValidator;
import com.airesume.server.dto.admin.GrowthConfigCreateRequest;
import com.airesume.server.dto.admin.GrowthConfigResponse;
import com.airesume.server.dto.admin.GrowthConfigUpdateRequest;
import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.service.SysGrowthConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/growth-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGrowthConfigController {

    private final SysGrowthConfigService sysGrowthConfigService;

    @GetMapping
    public Result<Map<String, Object>> getConfigList(
            @RequestParam(required = false) String groupName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get growth config list, groupName: {}, page: {}, size: {}", groupName, page, size);
        Page<SysGrowthConfig> pageParam = new Page<>(page, size);
        Page<SysGrowthConfig> result;
        if (groupName != null && !groupName.isBlank()) {
            result = sysGrowthConfigService.lambdaQuery()
                    .eq(SysGrowthConfig::getGroupName, groupName)
                    .orderByAsc(SysGrowthConfig::getSort)
                    .page(pageParam);
        } else {
            result = sysGrowthConfigService.lambdaQuery()
                    .orderByAsc(SysGrowthConfig::getGroupName)
                    .orderByAsc(SysGrowthConfig::getSort)
                    .page(pageParam);
        }
        List<GrowthConfigResponse> records = result.getRecords().stream()
                .map(this::buildResponse).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @PostMapping
    public Result<Long> createConfig(@Valid @RequestBody GrowthConfigCreateRequest request,
                                      Authentication authentication) {
        log.info("Admin create growth config, key: {}, group: {}", request.getConfigKey(), request.getGroupName());

        boolean exists = sysGrowthConfigService.lambdaQuery()
                .eq(SysGrowthConfig::getConfigKey, request.getConfigKey().trim())
                .count() > 0;
        if (exists) {
            throw new BusinessException("配置键已存在");
        }

        SysGrowthConfig config = new SysGrowthConfig();
        config.setConfigKey(request.getConfigKey().trim());
        config.setConfigValue(request.getConfigValue().trim());
        config.setDescription(request.getDescription());
        config.setGroupName(request.getGroupName() != null ? request.getGroupName() : "default");
        config.setSort(request.getSort() != null ? request.getSort() : 0);
        sysGrowthConfigService.save(config);

        log.info("Growth config created, id: {}", config.getId());
        return Result.success("配置创建成功", config.getId());
    }

    @PutMapping
    public Result<Void> updateConfig(@Valid @RequestBody GrowthConfigUpdateRequest request,
                                      Authentication authentication) {
        log.info("Admin update growth config, id: {}", request.getId());
        SysGrowthConfig config = sysGrowthConfigService.getById(request.getId());
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        if (request.getConfigKey() != null) {
            boolean exists = sysGrowthConfigService.lambdaQuery()
                    .eq(SysGrowthConfig::getConfigKey, request.getConfigKey().trim())
                    .ne(SysGrowthConfig::getId, request.getId())
                    .count() > 0;
            if (exists) throw new BusinessException("配置键已存在");
            config.setConfigKey(request.getConfigKey().trim());
        }
        if (request.getConfigValue() != null) config.setConfigValue(request.getConfigValue().trim());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getGroupName() != null) config.setGroupName(request.getGroupName());
        if (request.getSort() != null) config.setSort(request.getSort());

        sysGrowthConfigService.updateById(config);
        return Result.success("配置更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id, Authentication authentication) {
        log.info("Admin delete growth config, id: {}", id);
        sysGrowthConfigService.removeById(id);
        return Result.success("配置删除成功", null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> deleteConfigsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch delete growth configs, ids: {}", safeIds);
        sysGrowthConfigService.removeByIds(safeIds);
        return Result.success("配置批量删除成功", null);
    }

    private GrowthConfigResponse buildResponse(SysGrowthConfig entity) {
        return GrowthConfigResponse.builder()
                .id(entity.getId())
                .configKey(entity.getConfigKey())
                .configValue(entity.getConfigValue())
                .description(entity.getDescription())
                .groupName(entity.getGroupName())
                .sort(entity.getSort())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
