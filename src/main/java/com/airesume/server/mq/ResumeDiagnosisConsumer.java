package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.mock.MockDiagnosisResultGenerator;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 简历诊断任务消息消费者
 * 负责监听并处理简历诊断任务队列中的消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisConsumer {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final MockDiagnosisResultGenerator mockDiagnosisResultGenerator;

    /**
     * 监听简历诊断任务队列并处理消息
     *
     * @param message 任务消息
     * @param channel RabbitMQ Channel
     * @param deliveryTag 消息投递标签
     */
    @RabbitListener(queues = ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS)
    public void handleResumeDiagnosisTask(ResumeDiagnosisMessage message,
                                            Channel channel,
                                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long taskId = message.getTaskId();
        log.info("Received resume diagnosis task, taskId: {}, userId: {}", taskId, message.getUserId());

        try {
            // 1. 更新任务状态为处理中
            resumeDiagnosisTaskService.updateStatusToProcessing(taskId);

            // 2. 模拟处理延迟（模拟真实AI处理耗时）
            simulateProcessingDelay();

            // 3. 生成模拟诊断结果（替代真实AI调用）
            String mockResult = mockDiagnosisResultGenerator.generateMockDiagnosisResult(message.getFileUrl());

            // 4. 更新任务状态为完成并保存结果
            resumeDiagnosisTaskService.updateStatusToCompleted(taskId, mockResult);

            // 5. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("Resume diagnosis task processed successfully, taskId: {}", taskId);

        } catch (Exception e) {
            log.error("Failed to process resume diagnosis task, taskId: {}", taskId, e);
            try {
                // 更新任务状态为失败
                resumeDiagnosisTaskService.updateStatusToFailed(taskId, e.getMessage());

                // 确认消息（不重新入队，避免无限重试）
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("Failed to update task status or acknowledge message, taskId: {}", taskId, ex);
                try {
                    // 消息重新入队
                    channel.basicNack(deliveryTag, false, true);
                } catch (Exception nackEx) {
                    log.error("Failed to nack message, taskId: {}", taskId, nackEx);
                }
            }
        }
    }

    /**
     * 模拟处理延迟
     * 模拟真实AI处理简历所需的时间
     */
    private void simulateProcessingDelay() {
        try {
            // 随机延迟 2-5 秒
            int delayMs = 2000 + (int) (Math.random() * 3000);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Processing delay simulation interrupted");
        }
    }
}
