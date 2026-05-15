package com.airesume.server.controller;

import com.airesume.server.service.ResumePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 简历 PDF 导出控制器
 * 提供 HTML 转 PDF 的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumePdfController {

    static final int MAX_HTML_BYTES = 1024 * 1024;
    private static final Pattern DANGEROUS_BLOCK_TAGS = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|base|link|meta|form|input|button|textarea|select)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>|<\\s*(script|iframe|object|embed|base|link|meta|form|input|button|textarea|select)\\b[^>]*?/?>");
    private static final Pattern EVENT_HANDLER_ATTRIBUTES = Pattern.compile("(?i)\\s+on[a-z0-9_-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern DANGEROUS_URL_ATTRIBUTES = Pattern.compile("(?i)\\s+(src|href|xlink:href|action|formaction)\\s*=\\s*(\"(?!data:image/)[^\"]*\"|'(?!data:image/)[^']*'|(?!(?:data:image/))[^\\s>]+)");
    private static final Pattern CSS_REMOTE_URLS = Pattern.compile("(?i)url\\s*\\(\\s*(['\"]?)(?:https?:|file:|ftp:|//|data:(?!image/))[^)]*\\1\\s*\\)");

    private final ResumePdfService resumePdfService;

    /**
     * 将简历 HTML 导出为 PDF
     *
     * @param request 包含 html 字段的请求体
     * @return PDF 文件二进制流
     */
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, String> request) {
        String html = request.get("html");
        if (html == null || html.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // HTML 会直接进入 Headless Chrome 渲染，先拦住超大请求体，避免拖垮渲染进程和临时文件。
        int htmlBytes = html.getBytes(StandardCharsets.UTF_8).length;
        if (htmlBytes > MAX_HTML_BYTES) {
            log.warn("拒绝超大 PDF 导出请求，HTML 字节数: {}", htmlBytes);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        log.info("收到 PDF 导出请求，HTML 长度: {}", html.length());

        String sanitizedHtml = sanitizeHtmlForPdf(html);

        try {
            byte[] pdfBytes = resumePdfService.generatePdfFromHtml(sanitizedHtml);

            // 从 HTML 中提取文件名（简单方案：用时间戳）
            String filename = "resume_" + System.currentTimeMillis() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);

        } catch (ResumePdfService.PdfGenerationException e) {
            log.error("PDF 导出失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    static String sanitizeHtmlForPdf(String html) {
        String sanitized = DANGEROUS_BLOCK_TAGS.matcher(html).replaceAll("");
        sanitized = EVENT_HANDLER_ATTRIBUTES.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_URL_ATTRIBUTES.matcher(sanitized).replaceAll("");
        sanitized = CSS_REMOTE_URLS.matcher(sanitized).replaceAll("url('')");
        return sanitized;
    }
}
