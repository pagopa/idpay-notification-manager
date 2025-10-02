package it.gov.pagopa.notification.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "rest-client.notification.email-notification")
@Data
public class EmailNotificationProperties {
    private Subject subject;

    @Data
    public static class Subject {
        private String ok;
        private String partial;

    }
}
