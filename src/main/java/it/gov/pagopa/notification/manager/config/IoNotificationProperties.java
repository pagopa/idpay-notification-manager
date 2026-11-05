package it.gov.pagopa.notification.manager.config;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "notification.manager.io")
@Getter
@Setter
public class IoNotificationProperties {

    private Map<String, Map<NotificationEvent, NotificationContent>> profiles = new HashMap<>();
}