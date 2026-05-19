package com.airesume.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PDF 导出配置
 * 通过 application.yml 中的 app.pdf 前缀读取配置
 */
@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfConfig {

    /**
     * Chrome/Chromium 可执行文件路径
     * 留空则自动查找系统安装的 Chrome
     */
    private String chromePath;

    /**
     * PDF 生成超时时间（毫秒）
     */
    private long timeoutMs = 30000;

    /**
     * 是否允许 Chrome 使用 --no-sandbox。生产环境应保持 false，并通过容器/系统沙箱隔离渲染进程。
     */
    private boolean noSandboxEnabled = false;

    /**
     * PDF 保存路径（导出后保存一份到本地）
     * 留空则默认保存到项目目录下的 exported-pdfs 文件夹
     */
    private String savePath;

    /**
     * PDF 文件最大保留时间（分钟），超过此时间的文件会被定时清理
     * 默认 10 分钟。设为 0 或负数则禁用自动清理。
     */
    private int maxRetentionMinutes = 10;

    /**
     * 清理任务执行间隔（毫秒），默认每 5 分钟执行一次
     */
    private long cleanupIntervalMs = 300000;

    public String getChromePath() {
        return chromePath;
    }

    public void setChromePath(String chromePath) {
        this.chromePath = chromePath;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isNoSandboxEnabled() {
        return noSandboxEnabled;
    }

    public void setNoSandboxEnabled(boolean noSandboxEnabled) {
        this.noSandboxEnabled = noSandboxEnabled;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public int getMaxRetentionMinutes() {
        return maxRetentionMinutes;
    }

    public void setMaxRetentionMinutes(int maxRetentionMinutes) {
        this.maxRetentionMinutes = maxRetentionMinutes;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
}
