package com.mp3.demo.desktop;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // =========================================================
    // Noms des queues
    // =========================================================
    public static final String QUEUE_SCANNER_TO_METADATA = "queue.scanner.to.metadata";
    /** Queue 3 : chansons ayant passé blacklist + durée → insertion en base */
    public static final String QUEUE_MP3_TO_DB           = "queue.mp3.to.db";
    /** Queue 4 : chansons ayant passé blacklist + durée → suppression du dossier musique */
    public static final String QUEUE_MP3_TO_DELETE       = "queue.mp3.to.delete";

    // =========================================================
    // Noms des exchanges
    // =========================================================
    public static final String EXCHANGE_MP3 = "exchange.mp3";

    // =========================================================
    // Routing keys
    // =========================================================
    public static final String ROUTING_SCANNER_TO_METADATA = "mp3.scanner.metadata";
    public static final String ROUTING_MP3_TO_DB           = "mp3.to.db";
    public static final String ROUTING_MP3_TO_DELETE       = "mp3.to.delete";

    // =========================================================
    // Déclaration des queues
    // =========================================================

    @Bean
    public Queue queueScannerToMetadata() {
        return QueueBuilder.durable(QUEUE_SCANNER_TO_METADATA).build();
    }

    @Bean
    public Queue queueMp3ToDb() {
        return QueueBuilder.durable(QUEUE_MP3_TO_DB).build();
    }

    @Bean
    public Queue queueMp3ToDelete() {
        return QueueBuilder.durable(QUEUE_MP3_TO_DELETE).build();
    }

    // =========================================================
    // Exchange
    // =========================================================

    @Bean
    public DirectExchange exchangeMp3() {
        return ExchangeBuilder.directExchange(EXCHANGE_MP3).durable(true).build();
    }

    // =========================================================
    // Bindings
    // =========================================================

    @Bean
    public Binding bindingScannerToMetadata(Queue queueScannerToMetadata, DirectExchange exchangeMp3) {
        return BindingBuilder.bind(queueScannerToMetadata).to(exchangeMp3).with(ROUTING_SCANNER_TO_METADATA);
    }

    @Bean
    public Binding bindingMp3ToDb(Queue queueMp3ToDb, DirectExchange exchangeMp3) {
        return BindingBuilder.bind(queueMp3ToDb).to(exchangeMp3).with(ROUTING_MP3_TO_DB);
    }

    @Bean
    public Binding bindingMp3ToDelete(Queue queueMp3ToDelete, DirectExchange exchangeMp3) {
        return BindingBuilder.bind(queueMp3ToDelete).to(exchangeMp3).with(ROUTING_MP3_TO_DELETE);
    }

    // =========================================================
    // Convertisseur JSON
    // =========================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // =========================================================
    // RabbitTemplate
    // =========================================================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // =========================================================
    // ListenerContainerFactory
    // =========================================================

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        return factory;
    }
}