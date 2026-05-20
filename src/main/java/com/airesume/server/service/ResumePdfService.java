package com.airesume.server.service;

import com.airesume.server.config.PdfConfig;
import com.airesume.server.dto.resume.PdfGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 简历 PDF 生成服务
 * 使用 Chrome/Chromium 命令行 --print-to-pdf 将 HTML 转换为文字型 PDF
 *
 * 重构说明（生成与下载分离）：
 * - generatePdfFromHtml() 生成 PDF 并持久化到磁盘，返回文件标识（fileId）
 * - getPdfFile() 根据 fileId 查找并返回文件路径，供下载接口使用
 * - 前端先调用生成接口拿到 fileId -> 弹窗确认 -> 再调下载接口获取文件
 */
@Service
public class ResumePdfService {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfService.class);

    private final PdfConfig pdfConfig;

    /**
     * 创建 PDF 服务实例。
     *
     * @param pdfConfig PDF 导出配置
     */
    public ResumePdfService(PdfConfig pdfConfig) {
        this.pdfConfig = pdfConfig;
    }

    /**
     * 将 HTML 内容转换为 PDF 并持久化到磁盘。
     *
     * @param html 完整的 HTML 文档字符串（包含 style 标签的完整页面）
     * @return PdfGenerationResult 包含 fileId、fileName、fileSize，前端用 fileId 调用下载接口
     */
    public PdfGenerationResult generatePdfFromHtml(String html) {
        log.info("开始生成 PDF，HTML 长度: {}", html.length());

        Path tempHtml = null;
        Path tempPdf = null;
        ExecutorService outputReaderExecutor = null;
        Process process = null;
        Path tempUserDataDir = null;

        try {
            // 创建临时 HTML 文件。
            tempHtml = Files.createTempFile("resume_export_", ".html");
            Files.write(tempHtml, html.getBytes(StandardCharsets.UTF_8));

            // 创建临时 PDF 输出文件。
            tempPdf = Files.createTempFile("resume_export_", ".pdf");

            // 创建临时 Chrome user-data-dir，避免与已运行的 Chrome 实例冲突。
            tempUserDataDir = Files.createTempDirectory("chrome_user_data_");

            String chromePath = findChromeExecutable();
            if (chromePath == null) {
                throw new PdfGenerationException("未找到 Chrome/Chromium 可执行文件，请安装 Chrome 或在配置中指定路径", null);
            }

            log.info("使用 Chrome 路径: {}", chromePath);

            String htmlUrl = tempHtml.toAbsolutePath().toUri().toString();
            String pdfOutput = tempPdf.toAbsolutePath().toString();

            log.info("Chrome HTML URL: {}, PDF 输出路径: {}", htmlUrl, pdfOutput);

            List<String> command = new ArrayList<>();
            command.add(chromePath);
            command.add("--headless");
            if (pdfConfig.isNoSandboxEnabled()) {
                command.add("--no-sandbox");
            }
            command.add("--disable-gpu");
            command.add("--disable-dev-shm-usage");
            command.add("--user-data-dir=" + tempUserDataDir.toAbsolutePath());
            command.add("--print-to-pdf=" + pdfOutput);
            command.add("--no-pdf-header-footer");
            command.add(htmlUrl);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            // 独立读取进程输出，避免 readAllBytes 阻塞导致 waitFor 超时失效。
            Process capturedProcess = process;
            outputReaderExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            Future<String> outputFuture = outputReaderExecutor.submit(
                    () -> new String(capturedProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
            );
            long timeoutMs = pdfConfig.getTimeoutMs();
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                throw new PdfGenerationException("Chrome 执行超时（" + timeoutMs + "ms），已强制终止", null);
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

            byte[] pdfBytes = Files.readAllBytes(tempPdf);
            if (pdfBytes.length == 0) {
                log.error("Chrome 生成的 PDF 文件为空，HTML 长度: {}, Chrome 输出: {}", html.length(), output);
                throw new PdfGenerationException("Chrome 生成了空的 PDF 文件，请检查 HTML 内容", null);
            }

            // 校验 PDF 魔数，避免返回损坏文件。
            if (pdfBytes.length < 5 || pdfBytes[0] != '%' || pdfBytes[1] != 'P'
                    || pdfBytes[2] != 'D' || pdfBytes[3] != 'F' || pdfBytes[4] != '-') {
                log.error("Chrome 生成的文件不是有效的 PDF，文件大小: {} bytes，前 5 字节: {}",
                        pdfBytes.length, new String(pdfBytes, 0, Math.min(pdfBytes.length, 5), StandardCharsets.UTF_8));
                throw new PdfGenerationException("Chrome 生成的 PDF 文件无效（非 PDF 格式）", null);
            }

            String fileId = saveToLocal(pdfBytes);
            String fileName = "resume-" + fileId + ".pdf";
            log.info("PDF 生成完成，fileId: {}, 文件名: {}, 大小: {} bytes", fileId, fileName, pdfBytes.length);
            return new PdfGenerationResult(fileId, fileName, pdfBytes.length);
        } catch (PdfGenerationException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            log.error("PDF 生成失败", e);
            throw new PdfGenerationException("PDF 生成失败: " + e.getMessage(), e);
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (outputReaderExecutor != null) {
                outputReaderExecutor.shutdownNow();
            }
            deleteTempFile(tempHtml);
            deleteTempFile(tempPdf);
            deleteTempDirectory(tempUserDataDir);
        }
    }

    /**
     * 根据 fileId 获取 PDF 文件路径。
     *
     * 安全注意事项：
     * - fileId 必须是纯数字时间戳格式（\d{17}），防止路径遍历攻击
     * - 只允许访问配置的 savePath 目录下的文件
     *
     * @param fileId 生成 PDF 时返回的文件标识（yyyyMMddHHmmssSSS 时间戳）
     * @return PDF 文件路径
     * @throws PdfGenerationException 文件不存在或 fileId 格式非法
     */
    public Path getPdfFile(String fileId) {
        if (!fileId.matches("\\d{17}")) {
            throw new PdfGenerationException("非法的文件标识: " + fileId, null);
        }

        String savePath = getSavePath();
        Path filePath = Path.of(savePath, "resume-" + fileId + ".pdf").normalize();

        if (!filePath.startsWith(Path.of(savePath).normalize())) {
            throw new PdfGenerationException("非法的文件路径: " + fileId, null);
        }

        if (!Files.exists(filePath)) {
            throw new PdfGenerationException("PDF 文件不存在或已过期: " + fileId, null);
        }

        return filePath;
    }

    /**
     * 持久化保存 PDF 到本地目录。
     *
     * @param pdfBytes PDF 字节数组
     * @return fileId（时间戳字符串，例如 "20260507163500123"）
     */
    private String saveToLocal(byte[] pdfBytes) {
        try {
            String savePath = getSavePath();

            Path saveDir = Path.of(savePath);
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }

            String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
            String fileName = "resume-" + timestamp + ".pdf";
            Path saveFile = saveDir.resolve(fileName);

            // 同毫秒并发请求时继续向后取时间戳，避免文件名冲突。
            while (Files.exists(saveFile)) {
                timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
                fileName = "resume-" + timestamp + ".pdf";
                saveFile = saveDir.resolve(fileName);
            }

            Files.write(saveFile, pdfBytes);

            log.info("PDF 已保存到本地: {}", saveFile.toAbsolutePath());
            return timestamp;
        } catch (IOException e) {
            log.warn("保存 PDF 到本地失败: {}", e.getMessage());
            throw new PdfGenerationException("保存 PDF 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 PDF 保存目录路径。
     *
     * @return PDF 保存目录
     */
    private String getSavePath() {
        String savePath = pdfConfig.getSavePath();
        if (savePath == null || savePath.isBlank()) {
            savePath = System.getProperty("user.dir") + "/exported-pdfs";
        }
        return savePath;
    }

    /**
     * 查找 Chrome/Chromium 兼容浏览器的可执行文件。
     *
     * @return 浏览器可执行文件路径；未找到则返回 null
     */
    private String findChromeExecutable() {
        String configuredPath = pdfConfig.getChromePath();
        if (configuredPath != null && !configuredPath.isBlank() && Files.exists(Path.of(configuredPath))) {
            return configuredPath;
        }

        String os = System.getProperty("os.name").toLowerCase();

        for (String path : getBrowserExecutableCandidates(os)) {
            if (Files.exists(Path.of(path))) {
                return path;
            }
        }

        for (String command : getBrowserExecutableCommands(os)) {
            Process process = null;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("where", command);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (process.waitFor() == 0 && !result.isEmpty()) {
                    return result.split("\\R")[0].trim();
                }
            } catch (Exception ignored) {
                // 继续尝试下一个浏览器候选。
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }

        return null;
    }

    /**
     * 返回当前操作系统下的浏览器可执行文件候选路径。
     *
     * @param os 操作系统名称
     * @return 候选路径列表
     */
    List<String> getBrowserExecutableCandidates(String os) {
        if (os.contains("win")) {
            return Arrays.asList(
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"
            );
        }

        if (os.contains("mac")) {
            return List.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        }

        return Arrays.asList(
                "/usr/bin/google-chrome",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/snap/bin/chromium"
        );
    }

    /**
     * 返回当前操作系统下 PATH 探测要尝试的浏览器命令。
     *
     * @param os 操作系统名称
     * @return 候选命令列表
     */
    List<String> getBrowserExecutableCommands(String os) {
        if (os.contains("win")) {
            return Arrays.asList("chrome.exe", "msedge.exe");
        }

        return Arrays.asList("google-chrome", "chromium-browser", "chromium");
    }

    /**
     * 删除临时文件。
     *
     * @param path 文件路径
     */
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
     * 删除临时目录。
     *
     * @param path 目录路径
     */
    private void deleteTempDirectory(Path path) {
        if (path != null) {
            try {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("删除临时目录文件失败: {}", p, e);
                            }
                        });
            } catch (IOException e) {
                log.warn("删除临时目录失败: {}", path, e);
            }
        }
    }

    /**
     * 定时清理过期 PDF 文件。
     *
     * 每隔 cleanupIntervalMs 毫秒执行一次，删除最后修改时间超过 maxRetentionMinutes 分钟的 PDF 文件。
     */
    @Scheduled(fixedDelayString = "${app.pdf.cleanup-interval-ms:300000}")
    public void cleanupExpiredPdfs() {
        int maxRetentionMinutes = pdfConfig.getMaxRetentionMinutes();
        if (maxRetentionMinutes <= 0) {
            return;
        }

        Path saveDir = Path.of(getSavePath());
        if (!Files.exists(saveDir) || !Files.isDirectory(saveDir)) {
            return;
        }

        Instant cutoff = Instant.now().minusSeconds(maxRetentionMinutes * 60L);
        int deleted = 0;

        try (var files = Files.list(saveDir)) {
            var toDelete = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".pdf"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            log.warn("无法读取文件修改时间: {}", p, e);
                            return false;
                        }
                    })
                    .toList();

            for (Path file : toDelete) {
                try {
                    Files.deleteIfExists(file);
                    deleted++;
                    log.info("已清理过期 PDF: {}", file.getFileName());
                } catch (IOException e) {
                    log.warn("清理过期 PDF 失败: {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.warn("列出 PDF 目录失败: {}", saveDir, e);
        }

        if (deleted > 0) {
            log.info("PDF 过期清理完成，共删除 {} 个文件", deleted);
        }
    }

    /**
     * PDF 生成异常。
     */
    public static class PdfGenerationException extends RuntimeException {

        /**
         * 创建 PDF 生成异常。
         *
         * @param message 错误信息
         * @param cause 原始异常
         */
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
