package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.PdfGenerationResult;
import com.airesume.server.service.ResumePdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumePdfControllerTest {

    @Mock
    private ResumePdfService resumePdfService;

    @Test
    void shouldRejectOversizedHtmlPayload() {
        ResumePdfController controller = new ResumePdfController(resumePdfService);
        String html = "a".repeat(ResumePdfController.MAX_HTML_BYTES + 1);

        Result<PdfGenerationResult> response = controller.exportPdf(Map.of("html", html));

        assertEquals(500, response.getCode());
        assertEquals("HTML 内容过大，无法生成 PDF", response.getMessage());
        verify(resumePdfService, never()).generatePdfFromHtml(html);
    }

    @Test
    void shouldAllowHtmlPayloadForFiveMbInlinePhoto() {
        int fiveMbImageAsBase64Bytes = ((5 * 1024 * 1024 + 2) / 3) * 4;

        assertTrue(ResumePdfController.MAX_HTML_BYTES > fiveMbImageAsBase64Bytes);
    }

    @Test
    void shouldGeneratePdfWhenHtmlPayloadIsWithinLimit() {
        ResumePdfController controller = new ResumePdfController(resumePdfService);
        String html = "<html><body><p>resume</p></body></html>";
        PdfGenerationResult pdfResult = new PdfGenerationResult("20260520010203004", "resume-20260520010203004.pdf", 3);
        when(resumePdfService.generatePdfFromHtml(html)).thenReturn(pdfResult);

        Result<PdfGenerationResult> response = controller.exportPdf(Map.of("html", html));

        assertEquals(200, response.getCode());
        assertEquals(pdfResult, response.getData());
        verify(resumePdfService).generatePdfFromHtml(html);
    }

    @Test
    void shouldPassSanitizedHtmlToPdfService() {
        ResumePdfController controller = new ResumePdfController(resumePdfService);
        String html = """
                <html><head><script>alert(1)</script><link href="https://evil.test/a.css"></head>
                <body onload="steal()"><img src="file:///etc/passwd"><p>ok</p></body></html>
                """;
        PdfGenerationResult pdfResult = new PdfGenerationResult("20260520010203004", "resume-20260520010203004.pdf", 3);
        when(resumePdfService.generatePdfFromHtml(org.mockito.ArgumentMatchers.anyString())).thenReturn(pdfResult);

        Result<PdfGenerationResult> response = controller.exportPdf(Map.of("html", html));

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(resumePdfService).generatePdfFromHtml(htmlCaptor.capture());
        String sanitized = htmlCaptor.getValue();
        assertFalse(sanitized.toLowerCase().contains("<script"));
        assertFalse(sanitized.toLowerCase().contains("<link"));
        assertFalse(sanitized.toLowerCase().contains("onload"));
        assertFalse(sanitized.contains("file:///"));
        assertFalse(sanitized.contains("https://evil.test"));
    }

    @Test
    void shouldStripDangerousHtmlBeforePdfRendering() {
        String html = """
                <html><head><script>alert(1)</script><link href="https://evil.test/a.css"></head>
                <body onload="steal()">
                <img src="file:///etc/passwd">
                <img src="http://169.254.169.254/latest/meta-data">
                <img srcset="//evil.test/a.png 1x, https://evil.test/b.png 2x">
                <video poster="https://evil.test/poster.png"></video>
                <p style="background:url(https://evil.test/a.png)">ok</p>
                </body></html>
                """;

        String sanitized = ResumePdfController.sanitizeHtmlForPdf(html);

        assertFalse(sanitized.toLowerCase().contains("<script"));
        assertFalse(sanitized.toLowerCase().contains("<link"));
        assertFalse(sanitized.toLowerCase().contains("onload"));
        assertFalse(sanitized.contains("file:///"));
        assertFalse(sanitized.contains("169.254.169.254"));
        assertFalse(sanitized.contains("srcset="));
        assertFalse(sanitized.contains("poster="));
        assertFalse(sanitized.contains("https://evil.test"));
    }

    @Test
    void shouldKeepDataImageAndNormalHrefBeforePdfRendering() {
        String html = """
                <html><body>
                <img src="data:image/png;base64,AAAA">
                <a href="https://example.com/profile">profile</a>
                <a href="javascript:alert(1)">bad</a>
                </body></html>
                """;

        String sanitized = ResumePdfController.sanitizeHtmlForPdf(html);

        assertFalse(sanitized.contains("javascript:"));
        assertTrue(sanitized.contains("src=\"data:image/png;base64,AAAA\""));
        assertTrue(sanitized.contains("href=\"https://example.com/profile\""));
    }
}
