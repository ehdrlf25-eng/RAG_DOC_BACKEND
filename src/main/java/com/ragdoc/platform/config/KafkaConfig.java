package com.ragdoc.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragdoc.platform.kafka.event.DocumentUploadedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Producer/Consumer 및 DLQ({@code document.uploaded.dlq}) 설정.
 * <p>
 * Consumer 처리 실패 시 지정 횟수 재시도 후 DLQ로 전달한다.
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildProducerProperties(null));
        config.remove(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        config.remove(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.toString().startsWith("spring.json"));
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, DocumentUploadedEvent> documentUploadedConsumerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildConsumerProperties(null));
        config.remove(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.toString().startsWith("spring.json"));
        JsonDeserializer<DocumentUploadedEvent> deserializer = new JsonDeserializer<>(DocumentUploadedEvent.class, objectMapper);
        deserializer.addTrustedPackages("com.ragdoc.platform.kafka.event");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentUploadedEvent> documentUploadedListenerContainerFactory(
            ConsumerFactory<String, DocumentUploadedEvent> documentUploadedConsumerFactory,
            DefaultErrorHandler documentUploadedErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DocumentUploadedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(documentUploadedConsumerFactory);
        factory.setCommonErrorHandler(documentUploadedErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler documentUploadedErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaProperties kafkaProperties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(kafkaProperties.topics().documentUploadedDlq(), record.partition())
        );
        FixedBackOff backOff = new FixedBackOff(
                kafkaProperties.consumer().retryIntervalMs(),
                kafkaProperties.consumer().maxAttempts()
        );
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
