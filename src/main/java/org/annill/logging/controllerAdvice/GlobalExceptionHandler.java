package org.annill.logging.controllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.model.HttpLogEvent;
import org.annill.logging.util.LoggingUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final LoggingUtils loggingUtils;

    public GlobalExceptionHandler(LoggingUtils loggingUtils) {
        this.loggingUtils = loggingUtils;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Internal Server Error";

        logError(request, ex, status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", errorMessage);
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, status);
    }

    private void logError(HttpServletRequest request, Exception ex, HttpStatus status) {
        try {
            HttpServletRequest currentRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

            loggingUtils.logHttpEvent(
                HttpLogEvent.builder().timestamp(LocalDateTime.now()).direction("Incoming").method(request.getMethod())
                    .url(request.getRequestURI()).statusCode(status.value()).errorMessage(ex.getMessage()).build());
        } catch (Exception loggingEx) {
            // Fallback на обычное логирование если что-то пошло не так
            loggingUtils.logEvent(
                AuditEvent.builder().timestamp(LocalDateTime.now()).logLevel("ERROR").eventType("error")
                    .correlationId("GLOBAL-ERROR-HANDLER").methodName("GlobalExceptionHandler.handleGlobalException")
                    .errorMessage("Error while logging exception: " + loggingEx.getMessage() + " Original exception: "
                        + ex.getMessage()).build(), LogManager.getLogger(GlobalExceptionHandler.class));
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex,
        HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(request, ex, status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, status);
    }

}
