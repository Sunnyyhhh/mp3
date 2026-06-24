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
    public static final String QUEUE_METADATA_TO_SENDER  = "queue.metadata.to.sender";

    // =========================================================
    // Noms des exchanges
    // =========================================================
    public static final String EXCHANGE_MP3 = "exchange.mp3";

    // =========================================================
    // Routing keys
    // =========================================================
    public static final String ROUTING_SCANNER_TO_METADATA = "mp3.scanner.metadata";
    public static final String ROUTING_METADATA_TO_SENDER  = "mp3.metadata.sender";

    // =========================================================
    // Déclaration des queues (durable = true : survivent au redémarrage RabbitMQ)
    // =========================================================

    @Bean
    public Queue queueScannerToMetadata() {
        return QueueBuilder
                .durable(QUEUE_SCANNER_TO_METADATA)
                .build();
    }

    @Bean
    public Queue queueMetadataToSender() {
        return QueueBuilder
                .durable(QUEUE_METADATA_TO_SENDER)
                .build();
    }

    // =========================================================
    // Déclaration de l'Exchange (Direct Exchange)
    // Un seul exchange pour toute l'application MP3
    // =========================================================

    @Bean
    public DirectExchange exchangeMp3() {
        return ExchangeBuilder
                .directExchange(EXCHANGE_MP3)
                .durable(true)
                .build();
    }

    // =========================================================
    // Bindings : relie chaque queue à l'exchange via une routing key
    // =========================================================

    @Bean
    public Binding bindingScannerToMetadata(Queue queueScannerToMetadata, DirectExchange exchangeMp3) {
        return BindingBuilder
                .bind(queueScannerToMetadata)
                .to(exchangeMp3)
                .with(ROUTING_SCANNER_TO_METADATA);
    }

    @Bean
    public Binding bindingMetadataToSender(Queue queueMetadataToSender, DirectExchange exchangeMp3) {
        return BindingBuilder
                .bind(queueMetadataToSender)
                .to(exchangeMp3)
                .with(ROUTING_METADATA_TO_SENDER);
    }

    // =========================================================
    // Convertisseur JSON
    // Les messages seront sérialisés/désérialisés en JSON automatiquement
    // =========================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // =========================================================
    // RabbitTemplate : utilisé pour ENVOYER des messages (Prog 1 et Prog 2)
    // On y branche le convertisseur JSON
    // =========================================================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // =========================================================
    // ListenerContainerFactory : utilisé pour RECEVOIR des messages (Prog 2 et Prog 3)
    // On y branche le convertisseur JSON
    // =========================================================

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());

        // Nombre de messages traités en parallèle (1 = séquentiel, safe pour commencer)
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);

        return factory;
    }
}