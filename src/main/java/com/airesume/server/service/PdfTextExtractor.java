package com.airesume.server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfTextExtractor {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\uFEFF]");

    /**
     * 最小有效文本长度阈值
     * 低于此长度的 PDF 大概率是图片型（扫描件），无法提取有效文本
     */
    private static final int MIN_TEXT_LENGTH = 50;

    public String extractText(String fileUrl) {
        String absolutePath = resolveAbsolutePath(fileUrl);

        validateFile(absolutePath);

        String rawText = loadAndExtract(absolutePath);

        String cleaned = cleanText(rawText);

        if (cleaned.isBlank()) {
            throw new PdfExtractionException("PDF 文本提取结果为空，请确认上传的是文本型 PDF（非扫描件/图片型）");
        }

        // 检测是否为图片型PDF：提取的文本过少，大概率是扫描件
        if (cleaned.length() < MIN_TEXT_LENGTH) {
            log.warn("PDF 文本过少(length={})，疑似图片型/扫描件 PDF: {}", cleaned.length(), fileUrl);
            throw new PdfExtractionException(
                    "当前 PDF 似乎是图片型/扫描件，无法提取文本。请上传由 Word、WPS 等软件直接导出的文本型 PDF，或使用 Chrome 浏览器「打印 → 另存为 PDF」生成的文件");
        }

        log.info("PDF 文本提取成功, fileUrl: {}, charCount: {}", fileUrl, cleaned.length());
        return cleaned;
    }

    private String resolveAbsolutePath(String fileUrl) {
        String normalized = fileUrl.replace("\\", "/");
        if (normalized.startsWith("/")) {
            return System.getProperty("user.dir") + normalized;
        }
        return normalized;
    }

    private void validateFile(String absolutePath) {
        Path path = Path.of(absolutePath);
        if (!Files.exists(path)) {
            throw new PdfExtractionException("PDF 文件不存在: " + absolutePath);
        }
        if (!Files.isReadable(path)) {
            throw new PdfExtractionException("PDF 文件不可读: " + absolutePath);
        }
    }

    private String loadAndExtract(String absolutePath) {
        try (PDDocument document = Loader.loadPDF(new File(absolutePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new PdfExtractionException("PDF 解析失败: " + absolutePath, e);
        }
    }

    private String cleanText(String raw) {
        if (raw == null) return "";

        // 移除C0控制字符（保留\n和\t），防止存入MySQL JSON列时报Invalid encoding
        String result = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        result = ZERO_WIDTH.matcher(result).replaceAll("");
        result = MULTI_SPACE.matcher(result).replaceAll(" ");
        result = result.replaceAll("\\r\\n|\\r", "\n");
        result = result.replaceAll("\t", " ");

        StringBuilder sb = new StringBuilder();
        boolean lastWasBlank = false;
        for (String line : result.split("\\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!lastWasBlank) {
                    sb.append('\n');
                    lastWasBlank = true;
                }
            } else {
                sb.append(trimmed).append('\n');
                lastWasBlank = false;
            }
        }
        return sb.toString().trim();
    }

    public static class PdfExtractionException extends RuntimeException {
        public PdfExtractionException(String message) {
            super(message);
        }

        public PdfExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
