package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STT 模型发现响应。
 * <p>
 * STT 只有单一 OpenAI 兼容形态，仅需发现模型列表，无音色 / 合成端点探测。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSttDiscoveryResponse {

    /** 是否成功（模型列表获取成功即视为成功） */
    private Boolean success;

    /** 前端展示消息 */
    private String message;

    /** 发现的 STT 模型列表 */
    private List<SttModelOption> models;

    /** 失败原因，成功时为空 */
    private String errorMessage;
}
