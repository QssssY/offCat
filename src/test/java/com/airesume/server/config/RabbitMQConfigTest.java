package com.airesume.server.config;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.mq.ResumeDiagnosisDlqConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMQConfigTest {

    @Test
    void shouldConfigureResumeDiagnosisQueueWithoutImmutableTtlAndWithDeadLetterRoute() {
        Queue queue = new RabbitMQConfig().resumeDiagnosisQueue();

        assertEquals(ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS, queue.getName());
        assertFalse(queue.getArguments().containsKey("x-message-ttl"));
        assertEquals(ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS_DLX,
                queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS_DLQ,
                queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void shouldExposeRabbitTemplateRetryTemplate() {
        RetryTemplate retryTemplate = new RabbitMQConfig().rabbitTemplateRetryTemplate();

        assertNotNull(retryTemplate);
    }

    @Test
    void shouldListenToResumeDiagnosisDeadLetterQueueWithoutChangingQueueNames() throws NoSuchMethodException {
        Method method = ResumeDiagnosisDlqConsumer.class.getMethod("observeDeadLetter", org.springframework.amqp.core.Message.class);
        RabbitListener listener = method.getAnnotation(RabbitListener.class);

        assertNotNull(listener);
        assertArrayEquals(new String[]{ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS_DLQ}, listener.queues());
    }
}
