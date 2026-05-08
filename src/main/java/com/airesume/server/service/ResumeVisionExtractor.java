package com.airesume.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 多模态识别适配层。
 * 只负责把图片页转换成文本，不承担诊断逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVisionExtractor {

    private final ResumeAiService resumeAiService;

    /**
     * 当前启用的简历引擎是否声明支持多模态。
     */
    public boolean isAvailable() {
        boolean available = resumeAiService.supportsVisionExtraction();
        log.debug("多模态识别可用性: {}", available);
        return available;
    }

    /**
     * 使用多模态模型识别单页图片内容。
     *
     * @param pageImage PDF 渲染后的页图像
     * @param pageNumber 页码，从 1 开始
     * @return 提取后的文本
     */
    public String extractText(BufferedImage pageImage, int pageNumber) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(pageImage, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            String dataUrl = "data:image/png;base64," + base64;
            log.info("多模态识别开始, pageNumber: {}, imageSize: {}KB", pageNumber, outputStream.size() / 1024);
            String text = resumeAiService.extractTextFromImage(dataUrl, "第 " + pageNumber + " 页");
            log.info("多模态识别完成, pageNumber: {}, textLength: {}", pageNumber, text != null ? text.length() : 0);
            return text;
        } catch (IOException e) {
            throw new RuntimeException("多模态识别图片编码失败: " + e.getMessage(), e);
        }
    }
}
