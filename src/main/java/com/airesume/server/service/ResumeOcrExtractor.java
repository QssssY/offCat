package com.airesume.server.service;

import com.airesume.server.config.ResumeParseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OCR 识别适配层。
 * 首版通过 Tesseract CLI 提供兜底能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeOcrExtractor {

    private final ResumeParseConfig resumeParseConfig;
    private final PdfTextExtractor pdfTextExtractor;

    /**
     * 当前是否允许使用 OCR 兜底。
     */
    public boolean isEnabled() {
        return resumeParseConfig.getOcr().isEnabled();
    }

    /**
     * 对单页图片执行 OCR 文本识别。
     *
     * @param pageImage PDF 渲染后的页图像
     * @return 识别文本
     */
    public String extractText(BufferedImage pageImage) {
        if (!isEnabled()) {
            throw new RuntimeException("OCR 未启用");
        }

        Path imagePath = null;
        Path outputBase = null;
        try {
            imagePath = Files.createTempFile("resume-ocr-", ".png");
            outputBase = Files.createTempFile("resume-ocr-out-", "");
            Files.deleteIfExists(outputBase);
            ImageIO.write(pageImage, "png", imagePath.toFile());

            List<String> command = List.of(
                    resumeParseConfig.getOcr().getCommand(),
                    imagePath.toString(),
                    outputBase.toString(),
                    "-l",
                    resumeParseConfig.getOcr().getLang()
            );

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // OCR 超时 30 秒，防止 Tesseract 卡死阻塞线程。
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Tesseract 执行超时（30秒），已强制终止");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Tesseract 执行失败: " + processOutput);
            }

            Path textPath = Path.of(outputBase.toString() + ".txt");
            String rawText = Files.exists(textPath)
                    ? Files.readString(textPath, StandardCharsets.UTF_8)
                    : "";
            return pdfTextExtractor.cleanText(rawText);
        } catch (IOException e) {
            throw new RuntimeException("OCR 文件处理失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OCR 执行被中断", e);
        } finally {
            deleteQuietly(imagePath);
            deleteQuietly(outputBase);
            if (outputBase != null) {
                deleteQuietly(Path.of(outputBase.toString() + ".txt"));
            }
        }
    }

    /**
     * 临时文件删除失败不影响主流程，只记录调试日志。
     */
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("删除 OCR 临时文件失败: {}", path, e);
        }
    }
}
