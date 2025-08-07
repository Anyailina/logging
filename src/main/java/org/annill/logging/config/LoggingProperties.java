package org.annill.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit")
public class LoggingProperties {

    private Audit audit = new Audit();
    private Http http = new Http();

    @Data
    public static class Audit {

        private boolean enabled = true;
        private boolean console = true;
        private File file = new File();
        private Kafka kafka = new Kafka();
    }

    @Data
    public static class Http {

        private boolean enabled = true;
        private boolean console = true;
        private File file = new File();
        private Kafka kafka = new Kafka();
        private String filePath = "logs/http.log";
        private String kafkaTopic = "http-logs";
    }

    @Data
    public static class File {

        private boolean enabled = false;
        private String path = "logs/audit.log";
        private int maxSizeMb = 1;
    }

    @Data
    public static class Kafka {

        private boolean enabled = false;
        private String topic = "audit-logs";
        private String bootstrapServers = "localhost:9092";
    }

    public boolean isConsole() {
        return audit.isConsole() || http.isConsole();
    }

    public File getFile() {
        return audit.getFile();
    }

    public Kafka getKafka() {
        return audit.getKafka();
    }

}
