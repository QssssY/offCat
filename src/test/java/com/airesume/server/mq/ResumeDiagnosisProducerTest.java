package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResumeDiagnosisProducerTest {

    @Test
    void shouldSetPerMessageTtlWhenSendingResumeDiagnosisTask() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ResumeDiagnosisProducer producer = new ResumeDiagnosisProducer(rabbitTemplate);

        producer.sendResumeDiagnosisTask(10L, 20L, "/uploads/resumes/a.pdf");

        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS),
                eq(ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS),
                eq(ResumeDiagnosisMessage.builder()
                        .taskId(10L)
                        .userId(20L)
                        .fileUrl("/uploads/resumes/a.pdf")
                        .build()),
                processorCaptor.capture()
        );

        Message message = new Message(new byte[0], new MessageProperties());
        Message processed = processorCaptor.getValue().postProcessMessage(message);
        assertEquals("3600000", processed.getMessageProperties().getExpiration());
    }
}
