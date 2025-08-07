package org.annill.logging;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.annill.logging.config.LoggingProperties;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.model.HttpLogEvent;
import org.annill.logging.util.LoggingUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LoggingUtilsTest {

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private Logger logger;

    private LoggingProperties properties;

    private LoggingUtils loggingUtils;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        properties = new LoggingProperties();
        properties.getAudit().setConsole(true);
        properties.getAudit().getFile().setEnabled(false);
        properties.getAudit().getKafka().setEnabled(true);

        properties.getHttp().setConsole(true);
        properties.getHttp().getFile().setEnabled(false);
        properties.getHttp().getKafka().setEnabled(true);

        loggingUtils = new LoggingUtils(kafkaTemplate, properties);
    }

    @Test
    void testLogEvent_consoleAndKafka() {
        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .logLevel("INFO")
            .eventType("start")
            .correlationId("cid")
            .methodName("testMethod")
            .args(new Object[]{"arg1"})
            .build();

        loggingUtils.logEvent(event, logger);

        verify(kafkaTemplate).send(eq(properties.getAudit().getKafka().getTopic()), anyString());
    }

    @Test
    void testLogHttpEvent_consoleAndKafka() {
        HttpLogEvent event = HttpLogEvent.builder()
            .timestamp(LocalDateTime.now())
            .correlationId("cid")
            .direction("Incoming")
            .method("GET")
            .url("/url")
            .statusCode(200)
            .headers(new HashMap<>())
            .build();

        loggingUtils.logHttpEvent(event);

        verify(kafkaTemplate).send(eq(properties.getHttp().getKafka().getTopic()), anyString());
    }

    @Test
    void testLogEvent_FileLogging() throws IOException {
        properties.getAudit().getFile().setEnabled(true);
        properties.getAudit().getFile().setPath("target/test-audit.log");

        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .logLevel("INFO")
            .eventType("start")
            .correlationId("cid")
            .methodName("testMethod")
            .args(new Object[]{"arg1"})
            .build();

        loggingUtils.logEvent(event, logger);

        Path path = Paths.get("target/test-audit.log");
        assertTrue(Files.exists(path));
        Files.deleteIfExists(path);
    }


}
