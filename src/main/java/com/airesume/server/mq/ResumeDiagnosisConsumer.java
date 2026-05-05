package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.service.impl.ResumeDiagnosisProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 简历诊断消息消费者。
 *
 * 从 RabbitMQ 队列中读取消息后委托给 ResumeDiagnosisProcessor 处理。
 * 当系统繁忙（直连异步路径已满）时，任务会走此 MQ 消费路径。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisConsumer {

    /** 共享处理器，包含完整的诊断流水线逻辑 */
    private final ResumeDiagnosisProcessor resumeDiagnosisProcessor;

    /**
     * 监听简历诊断队列，收到消息后委托给共享处理器执行。
     *
     * @param message 包含 taskId、userId、fileUrl 的消息体
     */
    @RabbitListener(queues = ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS)
    public void handleResumeDiagnosisTask(ResumeDiagnosisMessage message) {
        log.info("MQ Consumer 收到简历诊断任务, taskId: {}, userId: {}", message.getTaskId(), message.getUserId());
        resumeDiagnosisProcessor.processTask(message.getTaskId(), message.getUserId(), message.getFileUrl());
    }
}
