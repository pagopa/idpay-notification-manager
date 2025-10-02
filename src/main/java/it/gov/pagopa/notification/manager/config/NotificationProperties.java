package it.gov.pagopa.notification.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "notification.manager")
@Data
public class NotificationProperties {
    private Markdown markdown;
    private Subject subject;

    @Data
    public static class Markdown {
        private String okCta;
        private String doubleNewLine;
        private String okBel;
        private String okPartialBel;
    }

    @Data
    public static class Subject {
        private String okBel;
        private String okPartialBel;

    }
}
