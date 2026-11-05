package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationContentRenderer;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationPlaceholder;
import it.gov.pagopa.notification.manager.service.io.profile.NotificationProfileResolution;
import it.gov.pagopa.notification.manager.service.io.profile.NotificationProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.gov.pagopa.notification.manager.service.onboarding.BaseOnboardingNotification.sanitizeString;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoNotificationContentService {

    private final IoNotificationCatalog catalog;
    private final NotificationContentRenderer renderer;
    private final NotificationProfileResolver profileResolver;

    public Optional<NotificationContent> find(String profile, NotificationEvent event) {
        Optional<NotificationContent> content = catalog.find(profile, event);
        if (content.isEmpty()) {
            log.warn("[NOTIFY][IO][NOTIFICATION_CONTENT_NOT_CONFIGURED] profile={}, event={}",
                    sanitizeString(profile), event);
        }
        return content;
    }

    public <T> Optional<NotificationContent> findAndRender(
            String profile,
            NotificationEvent event,
            T source,
            Iterable<? extends NotificationPlaceholder<T>> placeholders
    ) {
        return find(profile, event).map(content -> renderer.render(content, source, placeholders));
    }

    public <T> Optional<NotificationContent> resolveProfileAndFindAndRender(
            String configuredProfile,
            NotificationEvent event,
            T source,
            Iterable<? extends NotificationPlaceholder<T>> placeholders
    ) {
        NotificationProfileResolution resolution = profileResolver.resolveByInputProfile(configuredProfile);
        if (!(resolution instanceof NotificationProfileResolution.Resolved(String profile))) {
            return Optional.empty();
        }
        return findAndRender(profile, event, source, placeholders);
    }

    public <T> Optional<NotificationContent> resolveProfileByInitiativeIdAndFindAndRender(
            String initiativeId,
            NotificationEvent event,
            T source,
            Iterable<? extends NotificationPlaceholder<T>> placeholders
    ) {
        NotificationProfileResolution resolution = profileResolver.resolveByInitiativeId(initiativeId);
        if (!(resolution instanceof NotificationProfileResolution.Resolved(String profile))) {
            return Optional.empty();
        }
        return findAndRender(profile, event, source, placeholders);
    }
}
