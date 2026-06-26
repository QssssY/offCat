package com.airesume.server.service;

import com.airesume.server.config.PdfConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumePdfServiceTest {

    @Test
    void shouldIncludeEdgeCandidatesWhenDetectingBrowserOnWindows() {
        PdfConfig pdfConfig = new PdfConfig();
        ResumePdfService service = new ResumePdfService(pdfConfig);

        List<String> candidates = service.getBrowserExecutableCandidates("windows 11");
        List<String> commands = service.getBrowserExecutableCommands("windows 11");

        assertTrue(candidates.contains("C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"));
        assertTrue(candidates.contains("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"));
        assertEquals(List.of("chrome.exe", "msedge.exe"), commands);
    }
}
