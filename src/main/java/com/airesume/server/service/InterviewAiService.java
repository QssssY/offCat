package com.airesume.server.service;

import java.util.List;
import org.reactivestreams.Publisher;

public interface InterviewAiService {

    String generateOpening(String jobRole, Integer difficulty);

    String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage);

    Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage);

    EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history);

    record ChatMessageItem(String role, String content) {}

    record EvaluationResult(int score, String evaluationReport) {}
}
