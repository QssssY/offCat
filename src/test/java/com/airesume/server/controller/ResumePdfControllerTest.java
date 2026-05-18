package com.airesume.server.controller;

import com.airesume.server.service.ResumePdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        ResponseEntity<byte[]> response = controller.exportPdf(Map.of("html", html));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        verify(resumePdfService, never()).generatePdfFromHtml(html);
    }

    @Test
    void shouldGeneratePdfWhenHtmlPayloadIsWithinLimit() {
        ResumePdfController controller = new ResumePdfController(resumePdfService);
        String html = "<html><body><p>resume</p></body></html>";
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(resumePdfService.generatePdfFromHtml(html)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = controller.exportPdf(Map.of("html", html));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(pdfBytes, response.getBody());
        verify(resumePdfService).generatePdfFromHtml(html);
    }

    @Test
    void shouldStripDangerousHtmlBeforePdfRendering() {
        String html = """
                <html><head><script>alert(1)</script><link href="https://evil.test/a.css"></head>
                <body onload="steal()"><img src="file:///etc/passwd"><p style="background:url(https://evil.test/a.png)">ok</p></body></html>
                """;

        String sanitized = ResumePdfController.sanitizeHtmlForPdf(html);

        assertFalse(sanitized.toLowerCase().contains("<script"));
        assertFalse(sanitized.toLowerCase().contains("<link"));
        assertFalse(sanitized.toLowerCase().contains("onload"));
        assertFalse(sanitized.contains("file:///"));
        assertFalse(sanitized.contains("https://evil.test"));
    }
}
