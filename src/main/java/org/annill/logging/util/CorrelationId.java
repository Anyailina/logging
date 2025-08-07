package org.annill.logging.util;

import java.util.UUID;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

@NoArgsConstructor
public class CorrelationId {

    private static final String MDC_KEY = "correlationId";

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

}

