package com.airesume.server.service;

import com.airesume.server.config.PdfConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 简历 PDF 生成服务
 * 使用 Chrome/Chromium 命令行 --print-to-pdf 将 HTML 转换为文字型 PDF
 */
@Service
public class ResumePdfService {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfService.class);

    private final PdfConfig pdfConfig;

    public ResumePdfService(PdfConfig pdfConfig) {
        this.pdfConfig = pdfConfig;
    }

    /**
     * 将 HTML 内容转换为 PDF
     *
     * @param html 完整的 HTML 文档字符串（包含 style 标签的完整页面）
     * @return PDF 文件的字节数组
     */
    public byte[] generatePdfFromHtml(String html) {
        log.info("开始生成 PDF，HTML 长度: {}", html.length());

        Path tempHtml = null;
        Path tempPdf = null;

        try {
            // 创建临时 HTML 文件
            tempHtml = Files.createTempFile("resume_export_", ".html");
            Files.write(tempHtml, html.getBytes(StandardCharsets.UTF_8));

            // 创建临时 PDF 输出文件
            tempPdf = Files.createTempFile("resume_export_", ".pdf");

            // 查找 Chrome 可执行文件
            String chromePath = findChromeExecutable();
            if (chromePath == null) {
                throw new PdfGenerationException("未找到 Chrome/Chromium 可执行文件，请安装 Chrome 或在配置中指定路径", null);
            }

            log.info("使用 Chrome 路径: {}", chromePath);

            // 构建 Chrome 命令
            ProcessBuilder pb = new ProcessBuilder(
                    chromePath,
                    "--headless",
                    "--no-sandbox",
                    "--disable-gpu",
                    "--disable-dev-shm-usage",
                    "--print-to-pdf=" + tempPdf.toAbsolutePath(),
                    "--print-to-pdf-no-header",
                    tempHtml.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出（避免阻塞）
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Chrome 执行失败，退出码: {}, 输出: {}", exitCode, output);
                throw new PdfGenerationException("Chrome 执行失败，退出码: " + exitCode, null);
            }

            // 读取生成的 PDF 文件
            byte[] pdfBytes = Files.readAllBytes(tempPdf);

            log.info("PDF 生成完成，文件大小: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (PdfGenerationException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            log.error("PDF 生成失败", e);
            throw new PdfGenerationException("PDF 生成失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            deleteTempFile(tempHtml);
            deleteTempFile(tempPdf);
        }
    }

    /**
     * 查找 Chrome/Chromium 可执行文件路径
     */
    private String findChromeExecutable() {
        // 优先使用配置的路径
        String configuredPath = pdfConfig.getChromePath();
        if (configuredPath != null && !configuredPath.isBlank()) {
            if (Files.exists(Path.of(configuredPath))) {
                return configuredPath;
            }
        }

        // 常见的 Chrome 路径
        String[] possiblePaths = {
                // Windows
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                // macOS
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                // Linux
                "/usr/bin/google-chrome",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/snap/bin/chromium"
        };

        for (String path : possiblePaths) {
            if (Files.exists(Path.of(path))) {
                return path;
            }
        }

        // 尝试从 PATH 中查找
        String os = System.getProperty("os.name").toLowerCase();
        String chromeCommand = os.contains("win") ? "chrome.exe" : "google-chrome";
        try {
            ProcessBuilder pb = new ProcessBuilder("where", chromeCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String result = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (p.waitFor() == 0 && !result.isEmpty()) {
                return result.split("\n")[0].trim();
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他方式
        }

        return null;
    }

    private void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", path, e);
            }
        }
    }

    /**
     * PDF 生成异常
     */
    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
