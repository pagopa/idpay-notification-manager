package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.config.ProfilesNotificationProperties;
import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Component
public class ProfilesNotificationCatalog {

    public static final String DEFAULT_PROFILE = "DEFAULT";

    private final ProfilesNotificationProperties properties;

    public ProfilesNotificationCatalog(ProfilesNotificationProperties properties) {
        this.properties = properties;

        log.info("[NOTIFY][CATALOG_LOADED] profiles={}", properties.getProfiles().keySet());
        if (!properties.getProfiles().containsKey(DEFAULT_PROFILE)) {
            throw new IllegalArgumentException("The DEFAULT notification profile must be declared");
        }
    }

    public boolean containsProfile(String profile) {
        return properties.getProfiles().containsKey(profile);
    }

    public Optional<String> resolveProfile(String configuredProfile) {
        String profile = StringUtils.hasText(configuredProfile)
                ? configuredProfile
                : DEFAULT_PROFILE;
        String sanitizedProfileForLog = sanitizeForLog(profile);
        if (!containsProfile(profile)) {
            log.warn("[NOTIFY][UNKNOWN_NOTIFICATION_PROFILE] profile={}", sanitizedProfileForLog);
            return Optional.empty();
        }
        log.info("[NOTIFY][PROFILE_RESOLVED] profile={}", sanitizedProfileForLog);
        return Optional.of(profile);
    }

    public Optional<NotificationContent> find(String profile, NotificationEvent event) {
        return Optional.ofNullable(properties.getProfiles().get(profile))
                .map(ProfilesNotificationProperties.ProfileConfig::getEvents)
                .map(events -> events.get(event));
    }

    public Optional<ProfilesNotificationProperties.ProfileConfigVariables> retrieveProfileVariables(String configuredProfile) {
        return resolveProfile(configuredProfile)
                .map(profile -> properties.getProfiles().get(profile))
                .map(ProfilesNotificationProperties.ProfileConfig::getVariables);
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\n', '_').replace('\r', '_');
    }
}