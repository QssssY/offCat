package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.PdfGenerationResult;
import com.airesume.server.service.ResumePdfService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 简历 PDF 导出控制器（生成与下载分离）
 *
 * - POST /api/resume/export-pdf：接收 HTML，生成 PDF 并持久化到磁盘，返回 JSON（含 fileId）
 * - GET  /api/resume/download-pdf/{fileId}：根据 fileId 读取磁盘文件，以附件形式返回文件流
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumePdfController {

    static final int MAX_HTML_BYTES = 8 * 1024 * 1024;
    private static final Pattern DANGEROUS_BLOCK_TAGS = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|base|link|meta|form|input|button|textarea|select)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>|<\\s*(script|iframe|object|embed|base|link|meta|form|input|button|textarea|select)\\b[^>]*?/?>");
    private static final Pattern EVENT_HANDLER_ATTRIBUTES = Pattern.compile("(?i)\\s+on[a-z0-9_-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    // URL 属性先移除脚本、本地文件和非图片 data 协议，避免危险协议进入 Chrome 渲染链路。
    private static final Pattern DANGEROUS_URL_SCHEMES = Pattern.compile(
            "(?i)\\s+(src|href|xlink:href|action|formaction)\\s*=\\s*" +
            "(\"\\s*(?:javascript|vbscript|file|data\\s*:(?!image/))[^\">]*\"" +
            "|'\\s*(?:javascript|vbscript|file|data\\s*:(?!image/))[^'>]*'" +
            "|(?:javascript|vbscript|file|data\\s*:(?!image/))[^\\s>]+)");
    // 资源加载属性不允许保留远程地址，防止无头 Chrome 生成 PDF 时访问内网或云元数据地址。
    private static final Pattern REMOTE_RESOURCE_ATTRIBUTES = Pattern.compile(
            "(?i)\\s+(src|srcset|poster)\\s*=\\s*" +
            "(\"[^\"]*(?:https?:|//|file:|ftp:|data\\s*:(?!image/))[^\"]*\"" +
            "|'[^']*(?:https?:|//|file:|ftp:|data\\s*:(?!image/))[^']*'" +
            "|[^\\s>]*(?:https?:|//|file:|ftp:|data\\s*:(?!image/))[^\\s>]*)");
    private static final Pattern CSS_REMOTE_URLS = Pattern.compile("(?i)url\\s*\\(\\s*(['\"]?)(?:https?:|file:|ftp:|//|data:(?!image/))[^)]*\\1\\s*\\)");

    private final ResumePdfService resumePdfService;

    /**
     * 步骤一：生成 PDF
     *
     * 接收前端发来的 HTML，调用 Chrome 无头浏览器生成文字型 PDF 并保存到磁盘。
     * 成功后返回 fileId，前端拿此 ID 弹窗确认后再调下载接口。
     *
     * 请求体：{ "html": "<!DOCTYPE html>..." }
     * 成功响应：{ "code": 200, "message": "PDF 生成成功", "data": { "fileId": "uuid", ... } }
     */
    @PostMapping("/export-pdf")
    public Result<PdfGenerationResult> exportPdf(@RequestBody Map<String, String> request) {
        String html = request.get("html");
        if (html == null || html.isBlank()) {
            return Result.error("html 参数不能为空");
        }
        int htmlBytes = html.getBytes(StandardCharsets.UTF_8).length;
        if (htmlBytes > MAX_HTML_BYTES) {
            log.warn("拒绝超大 PDF 导出请求，HTML 字节数: {}", htmlBytes);
            return Result.error("HTML 内容过大，无法生成 PDF");
        }

        log.info("收到 PDF 生成请求，HTML 长度: {}", html.length());

        try {
            PdfGenerationResult result = resumePdfService.generatePdfFromHtml(sanitizeHtmlForPdf(html));
            log.info("PDF 生成成功，fileId: {}, 大小: {} bytes", result.getFileId(), result.getFileSize());
            return Result.success("PDF 生成成功", result);
        } catch (ResumePdfService.PdfGenerationException e) {
            log.error("PDF 生成失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 步骤二：下载 PDF
     *
     * 根据 fileId 读取磁盘上的 PDF 文件，以附件形式返回给浏览器。
     * 内部已对 fileId 做 UUID 格式校验和路径穿越防御。
     */
    @GetMapping("/download-pdf/{fileId}")
    public void downloadPdf(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        log.info("收到 PDF 下载请求，fileId: {}", fileId);

        try {
            Path pdfPath = resumePdfService.getPdfFile(fileId);
            String fileName = pdfPath.getFileName().toString();

            String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(pdfPath)));
            // 禁止 Range 分片请求，防止 IDM 等下载器开多路并行连接引发超时断开
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "none");

            Files.copy(pdfPath, response.getOutputStream());

            log.info("PDF 下载完成，fileId: {}, 文件: {}", fileId, fileName);

        } catch (ResumePdfService.PdfGenerationException e) {
            log.warn("PDF 下载失败（找不到文件或非法ID），fileId: {}: {}", fileId, e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":404,\"message\":\"文件不存在或已过期\",\"data\":null}");
        }
    }

    static String sanitizeHtmlForPdf(String html) {
        String sanitized = DANGEROUS_BLOCK_TAGS.matcher(html).replaceAll("");
        sanitized = EVENT_HANDLER_ATTRIBUTES.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_URL_SCHEMES.matcher(sanitized).replaceAll("");
        sanitized = REMOTE_RESOURCE_ATTRIBUTES.matcher(sanitized).replaceAll("");
        return CSS_REMOTE_URLS.matcher(sanitized).replaceAll("url('')");
    }
}
