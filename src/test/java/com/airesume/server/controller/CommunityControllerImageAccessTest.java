package com.airesume.server.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.airesume.server.service.CommunityService;
import com.airesume.server.service.OssService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityControllerImageAccessTest {

    @Mock
    private CommunityService communityService;

    @Mock
    private OssService ossService;

    @Test
    void shouldRedirectToSignedUrlWithoutLoggingCredentialUrl() {
        String objectKey = "community/1/20260605/abcdef.jpg";
        String signedUrl = "https://oss.example.com/community/1/20260605/abcdef.jpg?Expires=3600&Signature=secret";
        when(ossService.isEnabled()).thenReturn(true);
        when(ossService.generateSignedUrl(objectKey)).thenReturn(signedUrl);

        Logger logger = (Logger) LoggerFactory.getLogger(CommunityController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            CommunityController controller = new CommunityController(communityService, ossService);
            ReflectionTestUtils.setField(controller, "allowMissingReferer", true);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Referer", "http://localhost:3000/community");

            ResponseEntity<Void> response = controller.getImage("/" + objectKey, request);

            assertEquals(HttpStatus.FOUND, response.getStatusCode());
            assertEquals(signedUrl, response.getHeaders().getFirst("Location"));
            boolean loggedSignedUrl = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains(signedUrl)
                            || message.contains("Signature=secret")
                            || message.contains("Expires=3600"));
            assertFalse(loggedSignedUrl, "日志不能包含 OSS 签名 URL 或签名参数");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldRejectMissingRefererByDefault() {
        CommunityController controller = new CommunityController(communityService, ossService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Void> response = controller.getImage(
                "/community/1/20260605/abcdef.jpg", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(ossService, never()).generateSignedUrl("community/1/20260605/abcdef.jpg");
    }

    @Test
    void shouldRejectMalformedRefererByDefault() {
        CommunityController controller = new CommunityController(communityService, ossService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "%%%");

        ResponseEntity<Void> response = controller.getImage(
                "/community/1/20260605/abcdef.jpg", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(ossService, never()).generateSignedUrl("community/1/20260605/abcdef.jpg");
    }

    @Test
    void shouldRejectForeignReferer() {
        CommunityController controller = new CommunityController(communityService, ossService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://evil.example/images");

        ResponseEntity<Void> response = controller.getImage(
                "/community/1/20260605/abcdef.jpg", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(ossService, never()).generateSignedUrl("community/1/20260605/abcdef.jpg");
    }
}
