package com.airesume.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Token 限制配置类
 *
 * 所属模块：AI Token 优化模块 - 配置层
 * 职责：集中管理 AI 调用过程中的 token 限制和压缩策略参数
 * 用途：通过 YAML 配置文件动态调整 token 阈值，控制 API 成本
 *
 * 【配置前缀】
 * 在 application.yml 中使用 app.ai.token-limit 前缀配置：
 * ```yaml
 * app:
 *   ai:
 *     token-limit:
 *       resume-diagnosis-max: 6000
 *       interview-round-max: 4000
 * ```
 *
 * 【配置说明】
 * - resumeDiagnosisMax：简历诊断最大输入 token（含系统 Prompt + 用户 Prompt + 简历文本）
 * - polishResumeMax：简历润色最大输入 token
 * - interviewRoundMax：面试单轮对话最大输入 token
 * - interviewEvaluationMax：面试评价报告最大输入 token
 * - contextWindowRatio：上下文窗口安全使用率（默认 0.8，留 20% 给输出）
 * - defaultMaxContextTokens：默认模型最大上下文 token（DeepSeek-V3 为 8192）
 * - compressionEnabled：是否启用输入压缩（默认 true）
 * - tokenLimitEnabled：是否启用 token 限制保护（默认 true）
 * - historySummaryThreshold：历史对话摘要触发阈值（超过该轮次开始压缩历史）
 * - recentMessagesToKeep：保留最近完整对话的轮数（不受压缩影响）
 * - aiSummaryEnabled：是否启用 AI 摘要（默认 true，false 则回退到截断模式）
 * - aiSummaryTimeoutMs：AI 摘要请求超时时间（毫秒，默认 30000）
 * - evaluationRecentMessagesToKeep：评价报告保留最近完整对话的消息数（默认 10）
 *
 * @author AI Resume Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.token-limit")
public class AiTokenLimitConfig {

    /** 简历诊断最大输入 token 数（含系统 Prompt + 用户 Prompt + 简历文本） */
    private int resumeDiagnosisMax = 6000;

    /** 简历润色最大输入 token 数，默认与诊断相同 */
    private int polishResumeMax = 6000;

    /** 面试单轮对话最大输入 token 数 */
    private int interviewRoundMax = 4000;

    /** 面试评价报告最大输入 token 数 */
    private int interviewEvaluationMax = 12000;

    /**
     * 上下文窗口安全使用率阈值
     * 超过此比例时触发截断，留有余量给模型输出
     * 默认 0.8 表示最多使用 80% 的上下文窗口
     */
    private double contextWindowRatio = 0.8;

    /** 默认模型最大上下文 token 数（适用于 DeepSeek-V3 等 8K 模型） */
    private int defaultMaxContextTokens = 16384;

    /** 是否启用输入压缩（true=启用，false=禁用，恢复原始行为） */
    private boolean compressionEnabled = true;

    /** 是否启用 token 限制保护（true=启用自动截断，false=禁用） */
    private boolean tokenLimitEnabled = true;

    /**
     * 历史对话摘要触发阈值
     * 当对话轮次超过此值时，触发历史对话压缩策略
     */
    private int historySummaryThreshold = 6;

    /**
     * 保留最近完整对话的轮数
     * 压缩历史时，始终保留最近 N 轮完整对话不被压缩
     * 确保面试官能获取最新的对话上下文
     */
    private int recentMessagesToKeep = 3;

    /** 是否启用 AI 摘要（true=用AI生成摘要，false=使用截断兜底） */
    private boolean aiSummaryEnabled = true;

    /** AI 摘要请求超时时间（毫秒），默认 30 秒 */
    private int aiSummaryTimeoutMs = 30000;

    /** 评价报告保留最近完整对话的消息数（比常规压缩多保留，确保评价有充分依据） */
    private int evaluationRecentMessagesToKeep = 10;

    /** 重新摘要间隔（消息数），每累积这么多新消息后重新调用 AI 摘要，默认 6（约 3 轮对话） */
    private int resummarizeInterval = 6;
}
