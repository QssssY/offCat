package com.airesume.server.common.util;

import com.airesume.server.common.exception.BusinessException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BatchValidator {

    private static final int MAX_BATCH_SIZE = 100;

    public static List<Long> validate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("批量操作对象不能为空");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("批量操作最多支持" + MAX_BATCH_SIZE + "条");
        }
        List<Long> safeIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (safeIds.isEmpty()) {
            throw new BusinessException("批量操作对象不能为空");
        }
        return safeIds;
    }
}
