package org.annill.logging.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.model.HttpLogEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ConditionalOnProperty(name = "audit.logging.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaLoggingConfig {

    private final LoggingProperties properties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafka().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, AuditEvent> auditProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerFactory().getConfigurationProperties());
    }

    @Bean
    public ProducerFactory<String, HttpLogEvent> httpProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerFactory().getConfigurationProperties());
    }

    @Bean
    @ConditionalOnProperty(name = "audit.logging.kafka.enabled", havingValue = "true")
    public KafkaTemplate<String, AuditEvent> auditKafkaTemplate() {
        return new KafkaTemplate<>(auditProducerFactory());
    }

    @Bean
    @ConditionalOnProperty(name = "audit.http.kafka.enabled", havingValue = "true")
    public KafkaTemplate<String, HttpLogEvent> httpKafkaTemplate() {
        return new KafkaTemplate<>(httpProducerFactory());
    }

}
