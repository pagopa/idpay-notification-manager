package it.gov.pagopa.notification.manager.service.email;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.EmailNotificationContent;
import it.gov.pagopa.notification.manager.service.ProfilesNotificationCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.gov.pagopa.notification.manager.constants.NotificationConstants.EmailTemplates.BAD_START_EMAIL_TEMPLATE;
import static it.gov.pagopa.notification.manager.utils.Utilities.sanitizeString;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationContentService {

    private final ProfilesNotificationCatalog catalog;

    private Optional<EmailNotificationContent> find(String profile, NotificationEvent event) {
        Optional<EmailNotificationContent> content = catalog.findEmailNotificationContent(profile, event);
        if (content.isEmpty()) {
            log.warn("[NOTIFY][EMAIL][NOTIFICATION_CONTENT_NOT_CONFIGURED] profile={}, event={}",
                    sanitizeString(profile), event);
        }
        return content;
    }

    public Optional<EmailNotificationContent> resolveProfileAndFind(
            String configuredProfile,
            NotificationEvent event
    ) {
        return catalog.resolveProfile(configuredProfile)
                .flatMap(profile -> find(profile, event));
    }

    /**
     * Returns:
     *  - a: the content if found for the profile and event;
     *  - b: a fallback content if no content is found for the DEFAULT profile, which could be computed from a null
     *    configured profile because of an error as well;
     *  - c: an empty optional if no content is found for a non-DEFAULT profile;
     * This way the caller can handle the cases differently and retry the b case.
     */
    public Optional<EmailNotificationContent> resolveProfileAndFindWithDefaultFallback(
            String configuredProfile,
            NotificationEvent event
    ) {
        return catalog.resolveProfile(configuredProfile)
                .flatMap(profile -> {
                    Optional<EmailNotificationContent> content = find(profile, event);

                    // handle the case where the default profile is used and no content is found, returning a KO content
                    if (ProfilesNotificationCatalog.DEFAULT_PROFILE.equals(profile) && content.isEmpty()) {
                        return Optional.of(buildFallbackSaveContent(event));
                    }

                    return content;
                });
    }

    private EmailNotificationContent buildFallbackSaveContent(NotificationEvent event) {
        return new EmailNotificationContent(
                "Fallback subject",
                BAD_START_EMAIL_TEMPLATE + "/" + event.name()
        );
    }
}
