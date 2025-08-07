package org.annill.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.annill.logging.annotation.AuditLog;
import org.annill.logging.aspect.AuditLogAspect;
import org.annill.logging.model.AuditEvent;
import org.annill.logging.util.LoggingUtils;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


class AuditLogAspectTest {

    @InjectMocks
    private AuditLogAspect aspect;

    @Mock
    private LoggingUtils loggingUtils;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Logger logger;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(DummyService.class);
        when(methodSignature.getName()).thenReturn("dummyMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 42});
    }

    @Test
    void testLogMethodSuccess() throws Throwable {
        AuditLog auditLog = createAuditLog("DEBUG");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logMethod(joinPoint, auditLog);

        verify(loggingUtils, times(2)).logEvent(any(AuditEvent.class), any(Logger.class));
        assertEquals("result", result);
    }

    @Test
    void testLogMethodThrowsException() throws Throwable {
        AuditLog auditLog = createAuditLog("DEBUG");
        RuntimeException ex = new RuntimeException("fail");
        when(joinPoint.proceed()).thenThrow(ex);

        assertThrows(RuntimeException.class, () -> aspect.logMethod(joinPoint, auditLog));

        verify(loggingUtils).logEvent(
            argThat(event -> "error".equals(event.getEventType())),
            any(Logger.class)
        );
    }

    @Test
    void testLogMethodWithNullArguments() throws Throwable {
        AuditLog auditLog = createAuditLog("INFO");
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logMethod(joinPoint, auditLog);

        verify(loggingUtils, times(2)).logEvent(any(AuditEvent.class), any(Logger.class));
        assertEquals("result", result);
    }

    @Test
    void testLogMethodWithDifferentLogLevels() throws Throwable {
        AuditLog auditLog = createAuditLog("WARN");
        when(joinPoint.proceed()).thenReturn("result");

        // ACT
        aspect.logMethod(joinPoint, auditLog);

        // ASSERT
        verify(loggingUtils, times(2)).logEvent(
            argThat(event -> "WARN".equals(event.getLogLevel())),
            any(Logger.class)
        );
    }

    private AuditLog createAuditLog(String logLevel) {
        return new AuditLog() {
            @Override
            public String logLevel() {
                return logLevel;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AuditLog.class;
            }
        };
    }

    static class DummyService {
    }
}