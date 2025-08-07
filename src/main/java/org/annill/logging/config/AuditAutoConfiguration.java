package org.annill.logging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
public class AuditAutoConfiguration {

    private final LoggingProperties properties;

    public AuditAutoConfiguration(LoggingProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "audit.logging.kafka.enabled", havingValue = "true")
    @ConditionalOnClass(KafkaTemplate.class)
    public KafkaLoggingConfig kafkaLoggingConfig() {
        return new KafkaLoggingConfig(properties);
    }

}
