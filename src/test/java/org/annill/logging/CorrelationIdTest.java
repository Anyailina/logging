package org.annill.logging;

import org.annill.logging.util.CorrelationId;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdTest {

    @Test
    void testGenerate() {
        String id1 = CorrelationId.generate();
        String id2 = CorrelationId.generate();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertTrue(id1.matches("[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}"));
    }

    @Test
    void testSetAndClear() {
        String id = CorrelationId.generate();
        CorrelationId.set(id);
        
        assertEquals(id, MDC.get("correlationId"));
        
        CorrelationId.clear();
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void testSetNull() {
        CorrelationId.set(null);
        assertNull(MDC.get("correlationId"));
    }
}