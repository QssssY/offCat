package com.airesume.server.service;

import com.airesume.server.config.PdfConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        ExecutorService outputReaderExecutor = null;
        Process process = null;

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
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            command.add("--headless");
            if (pdfConfig.isNoSandboxEnabled()) {
                command.add("--no-sandbox");
            }
            command.add("--disable-gpu");
            command.add("--disable-dev-shm-usage");
            command.add("--disable-background-networking");
            command.add("--disable-extensions");
            command.add("--disable-sync");
            command.add("--print-to-pdf=" + tempPdf.toAbsolutePath());
            command.add("--print-to-pdf-no-header");
            command.add(tempHtml.toAbsolutePath().toString());
            ProcessBuilder pb = new ProcessBuilder(command);

            pb.redirectErrorStream(true);
            process = pb.start();

            // 在独立线程中读取进程输出，避免 readAllBytes 阻塞导致 waitFor 超时无效
            Process capturedProcess = process;
            outputReaderExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            Future<String> outputFuture = outputReaderExecutor.submit(
                    () -> new String(capturedProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
            );
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                throw new PdfGenerationException("Chrome 执行超时（60秒），已强制终止", null);
            }
            String output;
            try {
                output = outputFuture.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException readEx) {
                String reason = readEx.getCause() != null ? readEx.getCause().getMessage() : readEx.getMessage();
                throw new PdfGenerationException("读取 Chrome 输出失败: " + reason, readEx);
            }
            int exitCode = process.exitValue();

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
            // 确保进程和线程池被释放，防止资源泄漏
            if (process != null) {
                process.destroy();
            }
            if (outputReaderExecutor != null) {
                outputReaderExecutor.shutdownNow();
            }
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
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("where", chromeCommand);
            pb.redirectErrorStream(true);
            p = pb.start();
            String result = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (p.waitFor() == 0 && !result.isEmpty()) {
                return result.split("\n")[0].trim();
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他方式
        } finally {
            // 确保进程资源被释放，防止进程泄漏
            if (p != null) {
                p.destroy();
            }
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
