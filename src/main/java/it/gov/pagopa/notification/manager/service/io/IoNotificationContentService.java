package it.gov.pagopa.notification.manager.service.io;

import it.gov.pagopa.notification.manager.enums.NotificationEvent;
import it.gov.pagopa.notification.manager.model.NotificationContent;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationContentRenderer;
import it.gov.pagopa.notification.manager.service.io.placeholder.NotificationPlaceholder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.gov.pagopa.notification.manager.utils.Utilities.sanitizeString;


@Service
@RequiredArgsConstructor
@Slf4j
public class IoNotificationContentService {

    private final ProfilesNotificationCatalog catalog;
    private final NotificationContentRenderer renderer;

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
        return catalog.resolveProfile(configuredProfile)
                .flatMap(profile -> findAndRender(profile, event, source, placeholders));
    }

    public Optional<NotificationContent> resolveProfileAndFind(
            String configuredProfile,
            NotificationEvent event
    ) {
        return catalog.resolveProfile(configuredProfile)
                .flatMap(profile -> find(profile, event));
    }
}
