package com.airesume.server.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端单个用户的自定义 AI 用量明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUserUsageStatResponse {

    private Long userId;
    private String username;
    private String nickname;
    private Integer totalCalls;
    @Builder.Default
    private List<CustomAiUsageTypeStatResponse> typeStats = new ArrayList<>();
}
