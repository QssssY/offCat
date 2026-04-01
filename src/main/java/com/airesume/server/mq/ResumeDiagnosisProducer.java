package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 简历诊断任务消息生产者
 * 负责将简历诊断任务发送到RabbitMQ队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送简历诊断任务到消息队列
     *
     * @param taskId  任务ID
     * @param userId  用户ID
     * @param fileUrl 简历文件地址
     */
    public void sendResumeDiagnosisTask(Long taskId, Long userId, String fileUrl) {
        ResumeDiagnosisMessage message = ResumeDiagnosisMessage.builder()
                .taskId(taskId)
                .userId(userId)
                .fileUrl(fileUrl)
                .build();

        log.info("Sending resume diagnosis task to queue, taskId: {}, userId: {}", taskId, userId);

        rabbitTemplate.convertAndSend(
                ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS,
                ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS,
                message
        );

        log.info("Resume diagnosis task sent successfully, taskId: {}", taskId);
    }
}
