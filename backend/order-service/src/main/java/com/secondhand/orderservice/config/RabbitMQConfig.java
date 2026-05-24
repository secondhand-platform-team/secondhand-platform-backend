package com.secondhand.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration — Order Service (Publisher)
 * 
 * Khai báo exchanges, queues và bindings cho:
 * - Notification events (ORDER_CREATED, ORDER_CANCELLED, ...)
 * - Wallet events (ESCROW_RELEASE, ESCROW_REFUND) — async thay vì REST sync
 */
@Configuration
public class RabbitMQConfig {

    // ====== Notification ======
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";

    // ====== Wallet (Escrow Release/Refund) ======
    public static final String WALLET_EXCHANGE = "wallet.exchange";
    public static final String WALLET_QUEUE = "wallet.queue";
    public static final String WALLET_ROUTING_KEY = "wallet.#";

    // ====== Notification Beans ======

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // ====== Wallet Beans ======

    @Bean
    public TopicExchange walletExchange() {
        return new TopicExchange(WALLET_EXCHANGE);
    }

    @Bean
    public Queue walletQueue() {
        return QueueBuilder.durable(WALLET_QUEUE).build();
    }

    @Bean
    public Binding walletBinding(Queue walletQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(walletQueue)
                .to(walletExchange).with(WALLET_ROUTING_KEY);
    }

    // ====== JSON Message Converter ======
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
