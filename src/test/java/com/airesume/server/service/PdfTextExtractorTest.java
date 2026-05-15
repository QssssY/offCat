package com.airesume.server.service;

import com.airesume.server.service.PdfTextExtractor.PdfExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void resolveAbsolutePathShouldRejectNonUploadUrl() {
        PdfExtractionException ex = assertThrows(PdfExtractionException.class,
                () -> extractor.resolveAbsolutePath("/etc/passwd"));
        assertTrue(ex.getMessage().contains("Illegal PDF file path"));
    }

    @Test
    void resolveAbsolutePathShouldRejectPathTraversalUrl() {
        PdfExtractionException ex = assertThrows(PdfExtractionException.class,
                () -> extractor.resolveAbsolutePath("/uploads/resumes/../../../etc/passwd"));
        assertTrue(ex.getMessage().contains("Illegal PDF file path"));
    }

    @Test
    void resolveAbsolutePathShouldRejectDoubleDotUrl() {
        PdfExtractionException ex = assertThrows(PdfExtractionException.class,
                () -> extractor.resolveAbsolutePath("/uploads/resumes/..\\..\\etc\\passwd"));
        assertTrue(ex.getMessage().contains("Illegal PDF file path"));
    }

    @Test
    void resolveAbsolutePathShouldRejectNullUrl() {
        PdfExtractionException ex = assertThrows(PdfExtractionException.class,
                () -> extractor.resolveAbsolutePath(null));
        assertTrue(ex.getMessage().contains("PDF file path is empty"));
    }

    @Test
    void resolveAbsolutePathShouldRejectBlankUrl() {
        PdfExtractionException ex = assertThrows(PdfExtractionException.class,
                () -> extractor.resolveAbsolutePath("   "));
        assertTrue(ex.getMessage().contains("PDF file path is empty"));
    }

    @Test
    void resolveAbsolutePathShouldAcceptNormalUrl() {
        String result = extractor.resolveAbsolutePath("/uploads/resumes/test.pdf");
        assertTrue(result.contains("uploads") && result.contains("resumes") && result.contains("test.pdf"));
        assertFalse(result.contains(".."));
    }

    @Test
    void cleanTextShouldRemoveZeroWidthChars() {
        String input = "hello\u200Bworld\u200Ctest";
        String result = extractor.cleanText(input);
        assertFalse(result.contains("\u200B"));
        assertFalse(result.contains("\u200C"));
    }

    @Test
    void cleanTextShouldCollapseMultipleSpaces() {
        String input = "hello    world   test";
        String result = extractor.cleanText(input);
        assertEquals("hello world test", result);
    }

    @Test
    void cleanTextShouldNormalizeNewlines() {
        String input = "line1\r\nline2\rline3";
        String result = extractor.cleanText(input);
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    void cleanTextShouldHandleNull() {
        assertEquals("", extractor.cleanText(null));
    }
}
