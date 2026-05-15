package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ResumeDiagnosisTaskServiceImplTest {

    private final ResumeDiagnosisTaskServiceImpl service = new ResumeDiagnosisTaskServiceImpl(null, null, null, null, null, null, null);

    @Test
    void sanitizeOriginalFilenameShouldStripPathTraversal() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        assertEquals("passwd", method.invoke(service, "../../etc/passwd"));
        assertEquals("test.pdf", method.invoke(service, "foo/bar/test.pdf"));
        assertEquals("evil.exe", method.invoke(service, "C:\\Windows\\evil.exe"));
        assertEquals("resume.pdf", method.invoke(service, "../resume.pdf"));
        assertEquals("file.pdf", method.invoke(service, "a/b/c/../file.pdf"));
    }

    @Test
    void sanitizeOriginalFilenameShouldRejectNull() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, (String) null));
        assertInstanceOf(BusinessException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("null"));
    }

    @Test
    void sanitizeOriginalFilenameShouldRejectBlank() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("sanitizeOriginalFilename", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, ""));
        assertInstanceOf(BusinessException.class, ex.getCause());
    }

    @Test
    void buildStoredResumeFileNameShouldProduceUuidFormat() throws Exception {
        Method method = ResumeDiagnosisTaskServiceImpl.class.getDeclaredMethod("buildStoredResumeFileName");
        method.setAccessible(true);

        String fileName = (String) method.invoke(service);
        assertTrue(fileName.matches("\\d+_[a-f0-9]+\\.pdf"), "Expected format: timestamp_uuid.pdf, got: " + fileName);
    }

    @Test
    void getTaskByIdIsAnnotatedWithCacheable() throws NoSuchMethodException {
        java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                .getMethod("getTaskById", Long.class, Long.class);
        org.springframework.cache.annotation.Cacheable annotation =
                method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
        assertNotNull(annotation);
        assertEquals("resume:task", annotation.value()[0]);
    }

    @Test
    void updateStatusMethodsAreAnnotatedWithCacheEvict() throws NoSuchMethodException {
        String[] methods = {"updateStatusToProcessing", "updateStatusToCompleted",
                "updateStatusToFailed", "updateTaskResumeText", "updateTaskResumeParseResult"};
        Class<?>[][] paramTypes = {
                {Long.class},
                {Long.class, String.class},
                {Long.class, String.class},
                {Long.class, String.class},
                {Long.class, String.class, String.class, String.class}
        };
        for (int i = 0; i < methods.length; i++) {
            java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                    .getMethod(methods[i], paramTypes[i]);
            org.springframework.cache.annotation.CacheEvict annotation =
                    method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(annotation, methods[i] + " should have @CacheEvict");
            assertEquals("resume:task", annotation.value()[0]);
            assertTrue(annotation.allEntries(), methods[i] + " should have allEntries = true");
        }
    }

    @Test
    void updateStatusToProcessingShouldReturnBoolean() throws NoSuchMethodException {
        java.lang.reflect.Method method = ResumeDiagnosisTaskServiceImpl.class
                .getMethod("updateStatusToProcessing", Long.class);
        assertEquals(boolean.class, method.getReturnType(),
                "updateStatusToProcessing must return boolean for claim check");
    }
}
