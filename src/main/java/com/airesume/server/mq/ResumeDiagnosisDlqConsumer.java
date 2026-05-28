package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 简历诊断死信队列观测消费者。
 *
 * 死信只记录消息体和头信息，不自动重试、不写数据库，避免失败消息进入二次失败链路。
 */
@Slf4j
@Component
public class ResumeDiagnosisDlqConsumer {

    /**
     * 监听简历诊断死信队列，帮助排查过期、拒绝或处理失败的诊断消息。
     *
     * @param message RabbitMQ 原始死信消息
     */
    @RabbitListener(queues = ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS_DLQ)
    public void observeDeadLetter(Message message) {
        log.warn("Resume diagnosis task moved to DLQ, body: {}, headers: {}",
                new String(message.getBody()),
                message.getMessageProperties().getHeaders());
    }
}
