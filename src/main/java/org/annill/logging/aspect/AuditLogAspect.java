package org.annill.logging.aspect;

import java.time.LocalDateTime;
import org.annill.logging.annotation.AuditLog;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.util.CorrelationId;
import org.annill.logging.util.LoggingUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(name = "audit.logging.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogAspect {

    @Autowired
    private LoggingUtils loggingUtils;

    @Around("@annotation(auditLog)")
    public Object logMethod(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String correlationId = CorrelationId.generate();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        Logger logger = LogManager.getLogger(signature.getDeclaringType());

        loggingUtils.logEvent(
            AuditEvent.builder()
                .timestamp(LocalDateTime.now())
                .logLevel(auditLog.logLevel())
                .eventType("start")
                .correlationId(correlationId)
                .methodName(methodName)
                .args(joinPoint.getArgs())
                .build(),
            logger
        );

        try {
            Object result = joinPoint.proceed();

            loggingUtils.logEvent(
                AuditEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .logLevel(auditLog.logLevel())
                    .eventType("end")
                    .correlationId(correlationId)
                    .methodName(methodName)
                    .result(result)
                    .build(),
                logger
            );

            return result;
        } catch (Exception e) {
            loggingUtils.logEvent(
                AuditEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .logLevel("ERROR")
                    .eventType("error")
                    .correlationId(correlationId)
                    .methodName(methodName)
                    .errorMessage(e.getMessage())
                    .build(),
                logger
            );
            throw e;
        }
    }

}
