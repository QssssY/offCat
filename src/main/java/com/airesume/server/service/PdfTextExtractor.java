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

    public String extractText(String fileUrl) {
        String absolutePath = resolveAbsolutePath(fileUrl);

        validateFile(absolutePath);

        String rawText = loadAndExtract(absolutePath);

        String cleaned = cleanText(rawText);

        if (cleaned.isBlank()) {
            throw new PdfExtractionException("PDF 文本提取结果为空: " + fileUrl);
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

        String result = ZERO_WIDTH.matcher(raw).replaceAll("");
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
