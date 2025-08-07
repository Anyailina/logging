package org.annill.logging.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class HttpLogEvent {

    private LocalDateTime timestamp;
    private String correlationId;
    private String direction;
    private String method;
    private String url;
    private int statusCode;
    private Map<String, String> headers;
    private String queryParams;
    private Object requestBody;
    private Object responseBody;
    private String errorMessage;

}
