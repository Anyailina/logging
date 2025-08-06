package org.annill.logging;

import org.springframework.boot.SpringApplication;

public class TestLoggingApplication {

    public static void main(String[] args) {
        SpringApplication.from(LoggingApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
