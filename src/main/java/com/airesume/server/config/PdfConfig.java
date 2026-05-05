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
}
