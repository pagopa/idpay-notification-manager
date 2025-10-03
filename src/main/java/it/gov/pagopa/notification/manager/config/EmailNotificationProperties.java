package it.gov.pagopa.notification.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "rest-client.notification.email-notification")
@Getter
@Setter
public class EmailNotificationProperties {
    private Subject subject;

    @Getter
    @Setter
    public static class Subject {
        private String ok;
        private String partial;

    }
}
