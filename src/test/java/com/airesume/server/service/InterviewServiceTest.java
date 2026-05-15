package com.airesume.server.service;

import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewMessageService interviewMessageService;
    @Mock private MockInterviewService mockInterviewService;
    @Mock private InterviewMessageRepository interviewMessageRepository;
    @Mock private InterviewAiService interviewAiService;
    @Mock private SysJobRoleService sysJobRoleService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private UserQuotaService userQuotaService;
    @Mock private MockInterviewJobTargetService mockInterviewJobTargetService;
    @Mock private NotificationService notificationService;
    @Mock private Executor aiAsyncExecutor;
    @Mock private Subscription subscription;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InterviewService interviewService;

    @Captor
    private ArgumentCaptor<InterviewChatLog> chatLogCaptor;
    @Captor
    private ArgumentCaptor<Runnable> timeoutCaptor;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(
                interviewSessionRepository, interviewMessageService, mockInterviewService,
                interviewMessageRepository, interviewAiService, objectMapper, sysJobRoleService,
                transactionTemplate, userQuotaService, mockInterviewJobTargetService,
                notificationService, aiAsyncExecutor);
    }

    @Test
    void subscribeAndWriteStreamShouldPersistBeforeDone() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onNext("Hello, ");
            subscriber.onNext("this is AI response");
            subscriber.onComplete();
        };

        doAnswer((Answer<Void>) invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof TransactionCallbackWithoutResult callback) {
                callback.doInTransaction(null);
            } else if (arg instanceof java.util.function.Consumer) {
                ((java.util.function.Consumer<Object>) arg).accept(null);
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(Mockito.any());

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(interviewMessageRepository).save(chatLogCaptor.capture());
        InterviewChatLog savedMessage = chatLogCaptor.getValue();
        assertEquals(sessionId, savedMessage.getSessionId());
        assertEquals("assistant", savedMessage.getMessageRole());
        assertEquals("Hello, this is AI response", savedMessage.getContent());
        assertEquals("Hello, this is AI response", savedMessage.getContent());

        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).complete();
    }

    @Test
    void subscribeAndWriteStreamShouldSendErrorOnPersistFailure() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder("AI response content");

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onNext("AI ");
            subscriber.onNext("response content");
            subscriber.onComplete();
        };

        doThrow(new RuntimeException("DB connection lost"))
                .when(transactionTemplate).executeWithoutResult(Mockito.any());

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).completeWithError(any(RuntimeException.class));
    }

    @Test
    void subscribeAndWriteStreamShouldHandleEmptyReply() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onComplete();
        };

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(interviewMessageRepository, never()).save(any());
        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).complete();
    }

    @Test
    void subscribeAndWriteStreamShouldCancelSubscriptionOnTimeout() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> subscriber.onSubscribe(subscription);

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(emitter).onTimeout(timeoutCaptor.capture());
        timeoutCaptor.getValue().run();

        verify(subscription).cancel();
        verify(emitter).complete();
        verify(interviewMessageRepository, never()).save(any());
    }
}
