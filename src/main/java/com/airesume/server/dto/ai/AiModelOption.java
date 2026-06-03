package com.airesume.server.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 兼容模型列表中的单个模型选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelOption {

    /**
     * 提交给上游模型接口的模型 ID。
     */
    private String id;

    /**
     * 前端下拉展示名称，当前与 ID 保持一致。
     */
    private String name;
}
