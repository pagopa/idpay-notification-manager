package it.gov.pagopa.notification.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "notification.manager")
@Getter
@Setter
public class NotificationProperties {
    private Markdown markdown;
    private Subject subject;

    @Getter
    @Setter
    public static class Markdown {
        private String okCta;
        private String doubleNewLine;
        private String okBel;
        private String okPartialBel;
    }

    @Getter
    @Setter
    public static class Subject {
        private String okBel;
        private String okPartialBel;

    }
}
