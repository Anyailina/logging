package org.annill.logging.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.annill.logging.config.LoggingProperties;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.model.HttpLogEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoggingUtils {

    private static final Logger LOGGER = LogManager.getLogger(LoggingUtils.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final AtomicLong FILE_COUNTER = new AtomicLong(0);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LoggingProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void logEvent(AuditEvent event, Logger methodLogger) {
        String correlationId = event.getCorrelationId();
        CorrelationId.set(correlationId);

        String logMessage = buildAuditLogMessage(event);
        String jsonMessage = convertToJson(event);

        if (properties.isConsole()) {
            logToConsole(logMessage, event.getLogLevel(), methodLogger);
        }

        if (properties.getFile().isEnabled()) {
            logToFile(logMessage, properties.getFile().getPath());
        }

        if (properties.getKafka().isEnabled() && kafkaTemplate != null) {
            logToKafka(jsonMessage, properties.getKafka().getTopic());
        }

        CorrelationId.clear();
    }

    public void logHttpEvent(HttpLogEvent event) {
        CorrelationId.set(event.getCorrelationId());

        String logMessage = buildHttpLogMessage(event);
        String jsonMessage = convertToJson(event);

        if (properties.getHttp().isConsole()) {
            logToConsole(logMessage, "INFO", LOGGER);
        }

        if (properties.getHttp().getFile().isEnabled()) {
            logToFile(logMessage, properties.getHttp().getFile().getPath());
        }

        if (properties.getHttp().getKafka().isEnabled() && kafkaTemplate != null) {
            logToKafka(jsonMessage, properties.getHttp().getKafka().getTopic());
        }

        CorrelationId.clear();
    }

    private String buildAuditLogMessage(AuditEvent event) {
        StringBuilder sb = new StringBuilder().append(event.getTimestamp().format(TIMESTAMP_FORMATTER)).append(" ")
            .append(event.getLogLevel()).append(" ").append(event.getEventType().toUpperCase()).append(" ")
            .append(event.getCorrelationId()).append(" ").append(event.getMethodName()).append(" ");

        if ("start".equals(event.getEventType())) {
            sb.append("args = ").append(arrayToString(event.getArgs()));
        } else if ("end".equals(event.getEventType())) {
            sb.append("result = ").append(objectToString(event.getResult()));
        } else if ("error".equals(event.getEventType())) {
            sb.append("error = ").append(event.getErrorMessage());
        }

        return sb.toString();
    }

    private String buildHttpLogMessage(HttpLogEvent event) {
        StringBuilder sb = new StringBuilder().append(event.getTimestamp().format(TIMESTAMP_FORMATTER)).append(" ")
            .append(event.getDirection()).append(" ").append(event.getMethod()).append(" ")
            .append(event.getStatusCode()).append(" ").append(event.getUrl());

        if (event.getQueryParams() != null && !event.getQueryParams().isEmpty()) {
            sb.append("?").append(event.getQueryParams());
        }

        if (event.getRequestBody() != null) {
            sb.append(" RequestBody = ").append(objectToString(event.getRequestBody()));
        }

        if (event.getResponseBody() != null) {
            sb.append(" ResponseBody = ").append(objectToString(event.getResponseBody()));
        }

        if (event.getErrorMessage() != null) {
            sb.append(" Error = ").append(event.getErrorMessage());
        }

        return sb.toString();
    }

    private void logToConsole(String message, String level, Logger logger) {
        switch (level.toUpperCase()) {
            case "TRACE":
                logger.trace(message);
                break;
            case "DEBUG":
                logger.debug(message);
                break;
            case "INFO":
                logger.info(message);
                break;
            case "WARN":
                logger.warn(message);
                break;
            case "ERROR":
                logger.error(message);
                break;
            default:
                logger.debug(message);
        }
    }

    private void logToFile(String message, String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            long fileSize = Files.exists(path) ? Files.size(path) : 0;
            long maxSizeBytes = properties.getFile().getMaxSizeMb() * 1024 * 1024;

            String currentFilePath = filePath;
            if (fileSize > maxSizeBytes) {
                currentFilePath = filePath + "." + FILE_COUNTER.incrementAndGet();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFilePath, true))) {
                writer.write(message);
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write to audit log file", e);
        }
    }

    private void logToKafka(String message, String topic) {
        try {
            kafkaTemplate.send(topic, message).whenComplete((result, ex) -> {
                if (ex == null) {
                    LOGGER.debug("Successfully sent message to Kafka");
                } else {
                    LOGGER.error("Failed to send message to Kafka", ex);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error sending message to Kafka", e);
        }
    }

    public String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting object to JSON", e);
            return "{}";
        }
    }

    private String arrayToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(objectToString(array[i]));
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String objectToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

}
