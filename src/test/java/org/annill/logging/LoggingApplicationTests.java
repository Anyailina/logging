package org.annill.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LoggingApplicationTests {

    @Test
    void contextLoads() {
    }

}
