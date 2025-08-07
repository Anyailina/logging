package org.annill.logging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.annill.logging.config.LoggingProperties;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.model.HttpLogEvent;
import org.annill.logging.util.LoggingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9092", "port=9092"
})
@DirtiesContext
@TestPropertySource(properties = {
    "audit.kafka.enabled=true",
    "audit.kafka.topic=test-audit-topic",
    "audit.http.kafka.enabled=true",
    "audit.http.kafka.topic=test-http-topic",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.group-id=test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "audit.logging.enabled=true",
    "http.logging.enabled=true"
})
@EnableConfigurationProperties(LoggingProperties.class)
@Import(LoggingUtils.class)
class KafkaIntegrationLoggingTest {

    @Autowired
    private LoggingUtils loggingUtils;

    @Autowired
    private LoggingProperties properties;

    @Autowired
    private KafkaTemplate<String, AuditEvent> auditKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, HttpLogEvent> httpKafkaTemplate;

    private static CountDownLatch httpLatch;
    private static CountDownLatch auditLatch;

    private static String receivedHttpMessage;
    private static String receivedAuditMessage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        httpLatch = new CountDownLatch(1);
        auditLatch = new CountDownLatch(1);
        receivedHttpMessage = null;
        receivedAuditMessage = null;
    }

    @AfterEach
    void tearDown() {
        // Reset Kafka templates for clean state
        httpKafkaTemplate.flush();
        auditKafkaTemplate.flush();
    }

    @KafkaListener(topics = "test-http-topic", groupId = "test-group")
    public void listenHttp(String message) {
        receivedHttpMessage = message;
        httpLatch.countDown();
    }

    @KafkaListener(topics = "test-audit-topic", groupId = "test-group")
    public void listenAudit(String message) {
        receivedAuditMessage = message;
        auditLatch.countDown();
    }

    @Test
    void testHttpEventKafkaLogging() throws Exception {
        HttpLogEvent event = HttpLogEvent.builder()
            .timestamp(LocalDateTime.now())
            .correlationId("test-http-correlation-id")
            .direction("Incoming")
            .method("GET")
            .url("/test")
            .statusCode(200)
            .build();

        loggingUtils.logHttpEvent(event);

        assertTrue(httpLatch.await(5, TimeUnit.SECONDS), "HTTP message not received in Kafka");
        assertNotNull(receivedHttpMessage, "Received HTTP message should not be null");

        JsonNode jsonNode = objectMapper.readTree(receivedHttpMessage);
        assertEquals(event.getCorrelationId(), jsonNode.get("correlationId").asText());
        assertEquals(event.getMethod(), jsonNode.get("method").asText());
        assertEquals(event.getUrl(), jsonNode.get("url").asText());
        assertEquals(event.getDirection(), jsonNode.get("direction").asText());
    }

    @Test
    void testAuditEventKafkaLogging() throws Exception {
        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .logLevel("INFO")
            .eventType("test")
            .correlationId("test-correlation-id")
            .methodName("testMethod")
            .args(new Object[]{"arg1", 123})
            .build();

        Logger logger = LogManager.getLogger(LoggingUtils.class);
        loggingUtils.logEvent(event, logger);

        assertTrue(auditLatch.await(5, TimeUnit.SECONDS), "Audit message not received in Kafka");
        assertNotNull(receivedAuditMessage, "Received Audit message should not be null");

        JsonNode jsonNode = objectMapper.readTree(receivedAuditMessage);
        assertEquals(event.getCorrelationId(), jsonNode.get("correlationId").asText());
        assertEquals(event.getMethodName(), jsonNode.get("methodName").asText());
        assertEquals(event.getEventType(), jsonNode.get("eventType").asText());
        assertTrue(jsonNode.get("args").isArray());
        assertEquals(2, jsonNode.get("args").size());
    }

    @Test
    void testKafkaProducerConfiguration() {
        assertTrue(properties.getAudit().getKafka().isEnabled(), "Kafka should be enabled for audit");
        assertTrue(properties.getHttp().getKafka().isEnabled(), "Kafka should be enabled for HTTP");
        assertEquals("test-audit-topic", properties.getAudit().getKafka().getTopic());
        assertEquals("test-http-topic", properties.getHttp().getKafka().getTopic());
    }

    @Test
    void testLargeMessageLogging() throws Exception {
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeMessage.append("test-data-").append(i).append(" ");
        }

        HttpLogEvent event = HttpLogEvent.builder()
            .timestamp(LocalDateTime.now())
            .correlationId("large-message-test")
            .direction("Incoming")
            .method("POST")
            .url("/large")
            .statusCode(200)
            .requestBody(largeMessage.toString())
            .build();

        loggingUtils.logHttpEvent(event);

        assertTrue(httpLatch.await(10, TimeUnit.SECONDS), "Large message not received");
        assertNotNull(receivedHttpMessage, "Received message should not be null");
        assertTrue(receivedHttpMessage.length() > 10000, "Message should be large");

        JsonNode jsonNode = objectMapper.readTree(receivedHttpMessage);
        assertEquals(largeMessage.toString(), jsonNode.get("requestBody").asText());
    }

    @Test
    void testSerializationDeserialization() throws JsonProcessingException {
        // Создаем ObjectMapper с поддержкой JavaTimeModule
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .logLevel("DEBUG")
            .eventType("serialization-test")
            .correlationId("serial-test-id")
            .methodName("serialMethod")
            .args(new Object[]{"test", 42})
            .build();

        String json = mapper.writeValueAsString(event);
        AuditEvent deserialized = mapper.readValue(json, AuditEvent.class);

        assertEquals(event.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(event.getMethodName(), deserialized.getMethodName());
        assertEquals(event.getEventType(), deserialized.getEventType());
        assertArrayEquals(event.getArgs(), deserialized.getArgs());
        assertEquals(event.getTimestamp(), deserialized.getTimestamp());
    }


    @Test
    void testKafkaErrorHandling() {
        // Simulate Kafka being down
        httpKafkaTemplate.setDefaultTopic("non-existent-topic");

        HttpLogEvent event = HttpLogEvent.builder()
            .timestamp(LocalDateTime.now())
            .correlationId("error-test")
            .direction("Incoming")
            .method("GET")
            .url("/error-test")
            .statusCode(200)
            .build();

        try {
            loggingUtils.logHttpEvent(event);
            // Should log error but not throw exception
            assertTrue(true);
        } catch (Exception e) {
            fail("Should handle Kafka errors gracefully");
        } finally {
            // Restore original topic
            httpKafkaTemplate.setDefaultTopic(properties.getHttp().getKafka().getTopic());
        }
    }

    @Test
    void testMessageOrdering() throws Exception {
        // Send multiple messages and verify order
        int messageCount = 5;
        CountDownLatch multiLatch = new CountDownLatch(messageCount);
        String[] receivedMessages = new String[messageCount];

        new Thread(() -> {
            for (int i = 0; i < messageCount; i++) {
                AuditEvent event = AuditEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .logLevel("INFO")
                    .eventType("order-test")
                    .correlationId("order-test-" + i)
                    .methodName("method" + i)
                    .build();

                loggingUtils.logEvent(event, LogManager.getLogger(LoggingUtils.class));
                try {
                    Thread.sleep(100); // Ensure slight delay between messages
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        for (int i = 0; i < messageCount; i++) {
            assertTrue(auditLatch.await(2, TimeUnit.SECONDS), "Message not received");
            receivedMessages[i] = receivedAuditMessage;
            auditLatch = new CountDownLatch(1); // Reset for next message
        }

        // Verify order
        for (int i = 0; i < messageCount; i++) {
            JsonNode node = objectMapper.readTree(receivedMessages[i]);
            assertEquals("order-test-" + i, node.get("correlationId").asText());
        }
    }
}