package org.annill.logging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AuditEvent.AuditEventBuilder.class)
public class AuditEvent {

    private LocalDateTime timestamp;
    private String logLevel;
    private String eventType;
    private String correlationId;
    private String methodName;
    private Object[] args;
    private Object result;
    private String errorMessage;

    @JsonPOJOBuilder(withPrefix = "")
    public static class AuditEventBuilder {

    }

    @JsonCreator
    public static AuditEvent create(
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("logLevel") String logLevel,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("methodName") String methodName,
        @JsonProperty("args") Object[] args,
        @JsonProperty("result") Object result,
        @JsonProperty("errorMessage") String errorMessage) {
        return AuditEvent.builder()
            .timestamp(timestamp)
            .logLevel(logLevel)
            .eventType(eventType)
            .correlationId(correlationId)
            .methodName(methodName)
            .args(args)
            .result(result)
            .errorMessage(errorMessage)
            .build();
    }

}
