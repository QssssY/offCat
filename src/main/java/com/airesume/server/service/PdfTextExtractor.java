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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PDF 原生文本提取服务。
 * 仅负责文件校验、页级文本提取和文本清洗，不再直接判定图片型 PDF 失败。
 */
@Service
@Slf4j
public class PdfTextExtractor {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\uFEFF]");

    /**
     * 提取整份 PDF 的原生文本。
     * 如果整份文档完全没有原生文本，则仍然抛出异常，由上层决定是否回退 OCR / 多模态。
     *
     * @param fileUrl 文件访问路径
     * @return 清洗后的整份文本
     */
    public String extractText(String fileUrl) {
        PdfDocumentText documentText = extractDocument(fileUrl);
        if (documentText.getText().isBlank()) {
            throw new PdfExtractionException("PDF 原生文本提取结果为空");
        }
        log.info("PDF 原生文本提取完成, fileUrl: {}, charCount: {}", fileUrl, documentText.getText().length());
        return documentText.getText();
    }

    /**
     * 提取整份 PDF 的页级文本结果。
     * 统一解析层会基于页级结果判断哪些页面继续走多模态或 OCR。
     *
     * @param fileUrl 文件访问路径
     * @return 页级文本与整份合并文本
     */
    public PdfDocumentText extractDocument(String fileUrl) {
        String absolutePath = resolveAbsolutePath(fileUrl);
        validateFile(absolutePath);

        try (PDDocument document = Loader.loadPDF(new File(absolutePath))) {
            return extractDocument(absolutePath, document);
        } catch (IOException e) {
            throw new PdfExtractionException("PDF 解析失败: " + absolutePath, e);
        }
    }

    /**
     * 从已加载的 PDDocument 提取页级文本，避免调用方重复加载文件。
     *
     * @param absolutePath 文件绝对路径，仅用于结果记录
     * @param document 已打开的 PDDocument，由调用方管理生命周期
     * @return 页级文本与整份合并文本
     */
    public PdfDocumentText extractDocument(String absolutePath, PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<String> pageTexts = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pageTexts.add(cleanText(stripper.getText(document)));
            }

            String mergedText = String.join("\n\n", pageTexts).trim();
            return new PdfDocumentText(absolutePath, pageTexts, mergedText);
        } catch (IOException e) {
            throw new PdfExtractionException("PDF 文本提取失败: " + absolutePath, e);
        }
    }

    /**
     * 统一清洗提取结果。
     * 原生文本、多模态和 OCR 三条链路都复用这套清洗规则。
     *
     * @param raw 原始文本
     * @return 清洗后的文本
     */
    public String cleanText(String raw) {
        if (raw == null) {
            return "";
        }

        // 移除控制字符，避免后续 JSON 落库或返回时出现非法编码问题。
        String result = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        result = ZERO_WIDTH.matcher(result).replaceAll("");
        result = MULTI_SPACE.matcher(result).replaceAll(" ");
        result = result.replaceAll("\\r\\n|\\r", "\n");
        result = result.replace('\t', ' ');

        StringBuilder builder = new StringBuilder();
        boolean lastWasBlank = false;
        for (String line : result.split("\\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!lastWasBlank) {
                    builder.append('\n');
                    lastWasBlank = true;
                }
            } else {
                builder.append(trimmed).append('\n');
                lastWasBlank = false;
            }
        }
        return builder.toString().trim();
    }

    /**
     * 统一把上传路径转换为本地绝对路径。
     */
    public String resolveAbsolutePath(String fileUrl) {
        String normalized = fileUrl.replace("\\", "/");
        if (normalized.startsWith("/")) {
            return System.getProperty("user.dir") + normalized;
        }
        return normalized;
    }

    /**
     * 提取前先校验文件存在且可读。
     */
    private void validateFile(String absolutePath) {
        Path path = Path.of(absolutePath);
        if (!Files.exists(path)) {
            throw new PdfExtractionException("PDF 文件不存在: " + absolutePath);
        }
        if (!Files.isReadable(path)) {
            throw new PdfExtractionException("PDF 文件不可读: " + absolutePath);
        }
    }

    /**
     * 页级文本提取结果。
     */
    public static class PdfDocumentText {
        private final String absolutePath;
        private final List<String> pageTexts;
        private final String text;

        public PdfDocumentText(String absolutePath, List<String> pageTexts, String text) {
            this.absolutePath = absolutePath;
            this.pageTexts = List.copyOf(pageTexts);
            this.text = text;
        }

        public String getAbsolutePath() {
            return absolutePath;
        }

        public List<String> getPageTexts() {
            return pageTexts;
        }

        public String getText() {
            return text;
        }
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
