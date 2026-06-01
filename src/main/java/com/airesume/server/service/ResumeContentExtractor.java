package com.airesume.server.service;

import com.airesume.server.config.ResumeParseConfig;
import com.airesume.server.service.resume.ResumeParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一简历内容解析服务。
 * 解析顺序固定为：文本直提 -> 多模态识别 -> OCR 兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeContentExtractor {

    private final PdfTextExtractor pdfTextExtractor;
    private final ResumeVisionExtractor resumeVisionExtractor;
    private final ResumeOcrExtractor resumeOcrExtractor;
    private final ResumeParseConfig resumeParseConfig;

    /**
     * 执行整份 PDF 的统一解析。
     * 只加载一次 PDF 文档，同时用于文本提取和页面渲染。
     *
     * @param fileUrl 简历文件路径
     * @return 解析后的文本与解析元信息
     */
    public ResumeParseResult extract(String fileUrl) {
        return extract(fileUrl, null, false, false);
    }

    /**
     * 使用指定用户上下文执行整份 PDF 解析。
     */
    public ResumeParseResult extract(String fileUrl, Long userId, boolean fallbackToPlatform) {
        return extract(fileUrl, userId, fallbackToPlatform, false);
    }

    /**
     * 使用任务锁定的 AI 上下文执行整份 PDF 解析，保证图片页多模态识别与诊断计费来源一致。
     */
    public ResumeParseResult extract(String fileUrl, Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        String absolutePath = pdfTextExtractor.resolveAbsolutePath(fileUrl);

        try (PDDocument document = Loader.loadPDF(new File(absolutePath))) {
            // 文本提取和页面渲染共用同一个 PDDocument，避免重复加载文件。
            PdfTextExtractor.PdfDocumentText documentText = pdfTextExtractor.extractDocument(absolutePath, document);
            if (documentText.getPageTexts().isEmpty()) {
                throw new PdfTextExtractor.PdfExtractionException("PDF 没有可解析的页面");
            }

            List<String> pageTexts = new ArrayList<>();
            List<String> pageModes = new ArrayList<>();
            boolean usedVisionUnavailableFallback = false;
            boolean usedVisionFailureFallback = false;

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < documentText.getPageTexts().size(); pageIndex++) {
                String nativeText = documentText.getPageTexts().get(pageIndex);
                if (isEnoughText(nativeText)) {
                    pageTexts.add(nativeText);
                    pageModes.add(ResumeParseResult.MODE_TEXT);
                    continue;
                }

                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, resumeParseConfig.getOcr().getDpi());
                PageParseOutcome outcome = parseImagePage(pageImage, pageIndex + 1,
                        userId, fallbackToPlatform, requireUserCustom);
                usedVisionUnavailableFallback |= outcome.usedVisionUnavailableFallback();
                usedVisionFailureFallback |= outcome.usedVisionFailureFallback();

                String parsedText = outcome.text();
                if ((parsedText == null || parsedText.isBlank()) && nativeText != null && !nativeText.isBlank()) {
                    // 图片页识别失败但仍存在少量原生文本时，保留原生文本避免整页内容丢失。
                    parsedText = nativeText;
                    outcome = new PageParseOutcome(parsedText, ResumeParseResult.MODE_TEXT, false, false);
                }
                if (parsedText == null || parsedText.isBlank()) {
                    throw new PdfTextExtractor.PdfExtractionException("第 " + (pageIndex + 1) + " 页无法提取有效文本");
                }

                pageTexts.add(parsedText);
                pageModes.add(outcome.mode());
            }

            String text = String.join("\n\n", pageTexts).trim();
            if (text.isBlank()) {
                throw new PdfTextExtractor.PdfExtractionException("PDF 解析结果为空");
            }

            String parseMode = resolveDocumentMode(pageModes);
            String parseMessage = buildParseMessage(parseMode, usedVisionUnavailableFallback, usedVisionFailureFallback);
            log.info("简历解析完成, fileUrl: {}, parseMode: {}, pageTotal: {}", fileUrl, parseMode, pageModes.size());
            return ResumeParseResult.builder()
                    .text(text)
                    .parseMode(parseMode)
                    .parseMessage(parseMessage)
                    .build();
        } catch (IOException e) {
            throw new PdfTextExtractor.PdfExtractionException("PDF 加载失败: " + absolutePath, e);
        }
    }

    /**
     * 页文本达到阈值时，直接沿用原有文本提取结果。
     */
    private boolean isEnoughText(String text) {
        return text != null && text.length() >= resumeParseConfig.getParse().getTextThreshold();
    }

    /**
     * 图片页默认优先走多模态，不可用或失败时回退 OCR。
     */
    private PageParseOutcome parseImagePage(BufferedImage pageImage, int pageNumber,
                                            Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        String imagePriority = resumeParseConfig.getParse().getImagePriority();
        boolean multimodalFirst = !"ocr-first".equalsIgnoreCase(imagePriority != null ? imagePriority : "");
        if (multimodalFirst) {
            VisionAttempt visionAttempt = tryVision(pageImage, pageNumber, userId, fallbackToPlatform, requireUserCustom);
            if (visionAttempt.outcome() != null) {
                return visionAttempt.outcome();
            }
            PageParseOutcome ocrOutcome = tryOcr(pageImage, visionAttempt.unavailable(), visionAttempt.failed());
            if (ocrOutcome != null) {
                return ocrOutcome;
            }
        } else {
            PageParseOutcome ocrOutcome = tryOcr(pageImage, false, false);
            if (ocrOutcome != null) {
                return ocrOutcome;
            }
            VisionAttempt visionAttempt = tryVision(pageImage, pageNumber, userId, fallbackToPlatform, requireUserCustom);
            if (visionAttempt.outcome() != null) {
                return visionAttempt.outcome();
            }
        }
        throw new PdfTextExtractor.PdfExtractionException("第 " + pageNumber + " 页图片识别失败（多模态与 OCR 均未返回有效文本）");
    }

    /**
     * 多模态识别仅在当前启用引擎声明支持时才会尝试。
     */
    private VisionAttempt tryVision(BufferedImage pageImage, int pageNumber,
                                    Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        if (!resumeVisionExtractor.isAvailable(userId, fallbackToPlatform, requireUserCustom)) {
            return new VisionAttempt(null, true, false);
        }
        try {
            String text = pdfTextExtractor.cleanText(resumeVisionExtractor.extractText(
                    pageImage, pageNumber, userId, fallbackToPlatform, requireUserCustom));
            if (text == null || text.isBlank()) {
                return new VisionAttempt(null, false, true);
            }
            return new VisionAttempt(
                    new PageParseOutcome(text, ResumeParseResult.MODE_MULTIMODAL, false, false),
                    false,
                    false
            );
        } catch (Exception e) {
            log.warn("多模态识别失败，将回退 OCR。page: {}", pageNumber, e);
            return new VisionAttempt(null, false, true);
        }
    }

    /**
     * OCR 作为稳定兜底能力，在多模态不可用或失败后生效。
     */
    private PageParseOutcome tryOcr(BufferedImage pageImage, boolean visionUnavailable, boolean visionFailure) {
        if (!resumeOcrExtractor.isEnabled()) {
            return null;
        }
        try {
            String text = resumeOcrExtractor.extractText(pageImage);
            if (text == null || text.isBlank()) {
                return null;
            }
            return new PageParseOutcome(text, ResumeParseResult.MODE_OCR, visionUnavailable, visionFailure);
        } catch (Exception e) {
            log.warn("OCR 识别失败", e);
            return null;
        }
    }

    /**
     * 根据页级模式归并整份文档的解析模式。
     */
    private String resolveDocumentMode(List<String> pageModes) {
        boolean hasText = pageModes.contains(ResumeParseResult.MODE_TEXT);
        boolean hasVision = pageModes.contains(ResumeParseResult.MODE_MULTIMODAL);
        boolean hasOcr = pageModes.contains(ResumeParseResult.MODE_OCR);
        int modeCount = (hasText ? 1 : 0) + (hasVision ? 1 : 0) + (hasOcr ? 1 : 0);
        if (modeCount > 1) {
            return ResumeParseResult.MODE_MIXED;
        }
        if (hasVision) {
            return ResumeParseResult.MODE_MULTIMODAL;
        }
        if (hasOcr) {
            return ResumeParseResult.MODE_OCR;
        }
        return ResumeParseResult.MODE_TEXT;
    }

    /**
     * 为结果页生成可读的解析来源提示。
     */
    private String buildParseMessage(String parseMode, boolean visionUnavailableFallback, boolean visionFailureFallback) {
        return switch (parseMode) {
            case ResumeParseResult.MODE_TEXT -> "已通过 PDF 原生文本提取完成解析";
            case ResumeParseResult.MODE_MULTIMODAL -> "检测到图片页，已通过多模态识别提取文本";
            case ResumeParseResult.MODE_OCR -> {
                if (visionFailureFallback) {
                    yield "多模态识别失败，已自动回退 OCR";
                }
                if (visionUnavailableFallback) {
                    yield "当前简历引擎未开启多模态，已自动回退 OCR";
                }
                yield "检测到图片页，已通过 OCR 识别提取文本";
            }
            default -> "文档包含文本页与图片页，已使用混合解析";
        };
    }

    /**
     * 单页解析结果。
     */
    private record PageParseOutcome(
            String text,
            String mode,
            boolean usedVisionUnavailableFallback,
            boolean usedVisionFailureFallback) {
    }

    /**
     * 多模态尝试结果，用来区分回退 OCR 的原因。
     */
    private record VisionAttempt(PageParseOutcome outcome, boolean unavailable, boolean failed) {
    }
}
