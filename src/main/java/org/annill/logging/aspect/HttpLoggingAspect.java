package org.annill.logging.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.annill.logging.model.HttpLogEvent;
import org.annill.logging.util.LoggingUtils;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@ConditionalOnProperty(name = "http.logging.enabled", havingValue = "true", matchIfMissing = true)
public class HttpLoggingAspect {

    @Autowired
    private LoggingUtils loggingUtils;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Pointcut("execution(* org.springframework.web.client.RestTemplate.*(..))")
    public void restTemplate() {
    }

    @Before("restController()")
    public void logIncomingRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        loggingUtils.logHttpEvent(
            HttpLogEvent.builder().timestamp(LocalDateTime.now()).direction("Incoming").method(request.getMethod())
                .url(request.getRequestURI()).headers(headers).queryParams(request.getQueryString()).build());
    }

    @AfterReturning(pointcut = "restController()", returning = "response")
    public void logIncomingResponse(Object response) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        loggingUtils.logHttpEvent(
            HttpLogEvent.builder().timestamp(LocalDateTime.now()).direction("Incoming").method(request.getMethod())
                .url(request.getRequestURI()).statusCode(200).responseBody(response).build());
    }

    @AfterThrowing(pointcut = "restController()", throwing = "ex")
    public void logIncomingError(Exception ex) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        loggingUtils.logHttpEvent(
            HttpLogEvent.builder().timestamp(LocalDateTime.now()).direction("Incoming").method(request.getMethod())
                .url(request.getRequestURI()).statusCode(500).errorMessage(ex.getMessage()).build());
    }

}
