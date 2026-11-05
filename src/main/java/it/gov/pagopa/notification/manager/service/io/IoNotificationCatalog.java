package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.config.IoNotificationProperties;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class IoNotificationCatalog {

    public static final String DEFAULT_PROFILE = "DEFAULT";

    private final IoNotificationProperties properties;

    public IoNotificationCatalog(IoNotificationProperties properties) {
        this.properties = properties;

        log.info("[NOTIFY][IO][CATALOG_LOADED] profiles={}", properties.getProfiles().keySet());
        if (!properties.getProfiles().containsKey(DEFAULT_PROFILE)) {
            throw new IllegalArgumentException("The DEFAULT IO notification profile must be declared");
        }
    }

    public boolean containsProfile(String profile) {
        return properties.getProfiles().containsKey(profile);
    }

    public Optional<NotificationContent> find(String profile, NotificationEvent event) {
        return Optional.ofNullable(properties.getProfiles().get(profile))
                .map(events -> events.get(event));
    }
}