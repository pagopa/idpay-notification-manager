package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "notification.manager")
@Getter
@Setter
public class ProfilesNotificationProperties {

    private Map<String, ProfileConfig> profiles = new HashMap<>();

    @Getter
    @Setter
    @ToString
    public static class ProfileConfig {
        private ProfileConfigVariables variables = new ProfileConfigVariables();
        private Map<NotificationEvent, NotificationContent> events = new EnumMap<>(NotificationEvent.class);
    }

    @Getter
    @Setter
    @ToString
    public static class ProfileConfigVariables {
        private String assistedLink;
    }
}