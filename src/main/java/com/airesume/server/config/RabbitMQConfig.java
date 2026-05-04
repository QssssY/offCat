package com.airesume.server.config;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置队列、交换机、绑定关系以及消息转换器
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(1);
        // 并发消费者数量：最小1个，最大3个，避免单个任务卡住导致整个队列阻塞
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        // 消费者异常时不重启整个容器，只拒绝单条消息
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // ==================== 简历诊断队列配置 ====================

    /**
     * 简历诊断队列（配置死信交换机，被拒绝的消息会进入死信队列而非丢失）
     */
    @Bean
    public Queue resumeDiagnosisQueue() {
        return QueueBuilder.durable(ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS)
                .withArgument("x-dead-letter-exchange", ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS_DLX)
                .withArgument("x-dead-letter-routing-key", ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS_DLQ)
                .build();
    }

    /**
     * 简历诊断交换机
     */
    @Bean
    public DirectExchange resumeDiagnosisExchange() {
        return ExchangeBuilder.directExchange(ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS).build();
    }

    /**
     * 绑定简历诊断队列到交换机
     */
    @Bean
    public Binding resumeDiagnosisBinding() {
        return BindingBuilder
                .bind(resumeDiagnosisQueue())
                .to(resumeDiagnosisExchange())
                .with(ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS);
    }

    // ==================== 死信队列配置 ====================

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange resumeDiagnosisDlxExchange() {
        return ExchangeBuilder.directExchange(ResumeDiagnosisConstants.EXCHANGE_RESUME_DIAGNOSIS_DLX).build();
    }

    /**
     * 死信队列：存放被拒绝或处理失败的消息，便于排查和手动重试
     */
    @Bean
    public Queue resumeDiagnosisDlq() {
        return QueueBuilder.durable(ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS_DLQ).build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding resumeDiagnosisDlqBinding() {
        return BindingBuilder
                .bind(resumeDiagnosisDlq())
                .to(resumeDiagnosisDlxExchange())
                .with(ResumeDiagnosisConstants.ROUTING_KEY_RESUME_DIAGNOSIS_DLQ);
    }

}
