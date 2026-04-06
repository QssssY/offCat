package com.airesume.server.service.impl;

import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.service.InterviewAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import org.reactivestreams.Publisher;

@Service("interviewAiService")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.interview.mode", havingValue = "mock", matchIfMissing = true)
public class MockInterviewAiServiceImpl implements InterviewAiService {

    private final MockInterviewService mockInterviewService;

    @Override
    public String generateOpening(String jobRole, Integer difficulty) {
        log.info("[MOCK] 生成面试开场白, jobRole: {}, difficulty: {}", jobRole, difficulty);
        return mockInterviewService.generateMockOpening(jobRole, difficulty);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage) {
        log.info("[MOCK] 生成面试官回复, sessionId: {}, historySize: {}, userMessageLength: {}",
                sessionId, history == null ? 0 : history.size(),
                userMessage == null ? 0 : userMessage.length());

        int messageCount = history == null ? 0 : history.size();
        return mockInterviewService.generateMockReply(sessionId, userMessage, messageCount);
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage) {
        log.info("[MOCK] 流式生成面试官回复, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());

        int messageCount = history == null ? 0 : history.size();
        String fullReply = mockInterviewService.generateMockReply(sessionId, userMessage, messageCount);

        return Flux.<String>create(sink -> {
            for (int i = 0; i < fullReply.length(); i++) {
                sink.next(fullReply.substring(i, i + 1));
                if (Thread.currentThread().isInterrupted()) {
                    sink.error(new RuntimeException("流式输出被中断"));
                    return;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sink.error(new RuntimeException("流式输出被中断"));
                    return;
                }
            }
            sink.complete();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        log.info("[MOCK] 生成面试评价, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());

        int score = mockInterviewService.generateMockScore(sessionId);
        String report = mockInterviewService.generateMockEvaluationReport(sessionId, score);

        return new EvaluationResult(score, report);
    }
}
