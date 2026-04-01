package com.airesume.server.config;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import org.springframework.amqp.core.*;
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

    // ==================== 简历诊断队列配置 ====================

    /**
     * 简历诊断队列
     */
    @Bean
    public Queue resumeDiagnosisQueue() {
        return QueueBuilder.durable(ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS).build();
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

}
